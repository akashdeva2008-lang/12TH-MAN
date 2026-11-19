"""
12THMAN GPT-1 Training Pipeline
Trains a decoder-only GPT model from scratch with randomly initialized weights.
Zero pretrained models or weights used.
"""

import os
import sys
import time
import math
import torch
import torch.optim as optim

current_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.dirname(current_dir)
if parent_dir not in sys.path:
    sys.path.insert(0, parent_dir)

from model.config import GPTConfig
from model.model import TwelveManGPT
from tokenizer.tokenizer import CricketTokenizer
from training.dataset_loader import CricketDataset


@torch.no_grad()
def estimate_loss(model, dataset, device, eval_iters=15, batch_size=8):
    out = {}
    model.eval()
    for split in ["train", "val"]:
        losses = torch.zeros(eval_iters)
        for k in range(eval_iters):
            X, Y = dataset.get_batch(split, batch_size=batch_size, device=device)
            _, loss = model(X, Y)
            losses[k] = loss.item()
        out[split] = losses.mean().item()
    model.train()
    return out


def train():
    device = "cuda" if torch.cuda.is_available() else "cpu"
    print(f"\n=======================================================")
    print(f"        12THMAN GPT-1 TRAINING PIPELINE")
    print(f"  Training from scratch with randomly initialized weights")
    print(f"  Target Device: {device.upper()}")
    print(f"=======================================================\n")

    # 1. Load Tokenizer
    tok_path = os.path.join(parent_dir, "checkpoints", "tokenizer.json")
    if not os.path.exists(tok_path):
        tok_path = os.path.join(parent_dir, "tokenizer", "tokenizer.json")

    tokenizer = CricketTokenizer.load(tok_path)
    print(f"[Training] Tokenizer loaded with vocabulary size: {tokenizer.vocab_size}")

    # 2. Prepare Dataset
    dataset_dir = os.path.join(parent_dir, "dataset")
    block_size = 128
    dataset = CricketDataset(dataset_dir, tokenizer, block_size=block_size)

    # 3. Model Configuration & Random Initialization
    config = GPTConfig(
        vocab_size=tokenizer.vocab_size,
        block_size=block_size,
        n_layer=4,           # 4 Transformer decoder layers
        n_head=4,            # 4 Attention heads
        n_embd=128,          # 128 Embedding dimensions
        dropout=0.1,
        bias=False
    )

    model = TwelveManGPT(config)
    model.to(device)

    # Save model config
    checkpoints_dir = os.path.join(parent_dir, "checkpoints")
    os.makedirs(checkpoints_dir, exist_ok=True)
    config.save(os.path.join(checkpoints_dir, "model_config.json"))

    # 4. Optimizer & Learning Rate Schedule
    learning_rate = 1e-3
    max_iters = 600
    eval_interval = 100
    batch_size = 8

    optimizer = optim.AdamW(model.parameters(), lr=learning_rate, weight_decay=1e-2, betas=(0.9, 0.95))
    scheduler = optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=max_iters, eta_min=1e-4)

    best_val_loss = float("inf")
    best_checkpoint_path = os.path.join(checkpoints_dir, "12thman_gpt1_best.pt")
    latest_checkpoint_path = os.path.join(checkpoints_dir, "12thman_gpt1_latest.pt")

    print(f"[Training] Beginning training for {max_iters} iterations...")
    start_time = time.time()

    for iter_num in range(1, max_iters + 1):
        # Sample training batch
        xb, yb = dataset.get_batch("train", batch_size=batch_size, device=device)

        # Forward pass & loss
        logits, loss = model(xb, yb)

        # Backward pass & optimization
        optimizer.zero_grad(set_to_none=True)
        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), max_norm=1.0)
        optimizer.step()
        scheduler.step()

        # Evaluation & Progress logging
        if iter_num % eval_interval == 0 or iter_num == 1 or iter_num == max_iters:
            losses = estimate_loss(model, dataset, device, eval_iters=15, batch_size=batch_size)
            train_loss = losses["train"]
            val_loss = losses["val"]
            perplexity = math.exp(min(val_loss, 20))
            elapsed = time.time() - start_time
            current_lr = scheduler.get_last_lr()[0]

            print(f"Step {iter_num:4d}/{max_iters} | Train Loss: {train_loss:.4f} | Val Loss: {val_loss:.4f} | "
                  f"Perplexity: {perplexity:.2f} | LR: {current_lr:.6f} | Elapsed: {elapsed:.1f}s")

            # Save best checkpoint
            if val_loss < best_val_loss:
                best_val_loss = val_loss
                torch.save({
                    "step": iter_num,
                    "model_state_dict": model.state_dict(),
                    "optimizer_state_dict": optimizer.state_dict(),
                    "val_loss": val_loss,
                    "config": config.to_dict()
                }, best_checkpoint_path)
                print(f"  ★ New best model checkpoint saved to: {best_checkpoint_path}")

    # Save final latest checkpoint
    torch.save({
        "step": max_iters,
        "model_state_dict": model.state_dict(),
        "optimizer_state_dict": optimizer.state_dict(),
        "val_loss": val_loss,
        "config": config.to_dict()
    }, latest_checkpoint_path)

    total_time = time.time() - start_time
    print(f"\n[Training Completed in {total_time:.2f} seconds]")
    print(f"Best Validation Loss: {best_val_loss:.4f}")
    print(f"Final Model Checkpoint: {best_checkpoint_path}")

    # 5. Quick Post-Training Generation Test
    print("\n--- Model Text Generation Test ---")
    model.eval()
    test_prompts = [
        "In cricket, an LBW occurs when",
        "A cover drive is",
        "What is the role of a 12th man?"
    ]

    for prompt in test_prompts:
        input_ids = tokenizer.encode(prompt, add_special_tokens=False)
        input_tensor = torch.tensor([input_ids], dtype=torch.long, device=device)
        generated_ids = model.generate(
            input_tensor,
            max_new_tokens=40,
            temperature=0.7,
            top_k=25,
            eos_token_id=tokenizer.eos_token_id
        )[0].tolist()

        generated_text = tokenizer.decode(generated_ids, skip_special_tokens=True)
        print(f"\nPrompt:    '{prompt}'")
        print(f"Generated: '{generated_text}'")


if __name__ == "__main__":
    train()
