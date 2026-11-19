"""
12THMAN GPT-1 Model Evaluation & Interactive Testing
"""

import os
import sys
import torch

current_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.dirname(current_dir)
if parent_dir not in sys.path:
    sys.path.insert(0, parent_dir)

from model.config import GPTConfig
from model.model import TwelveManGPT
from tokenizer.tokenizer import CricketTokenizer


def load_model_and_tokenizer(checkpoint_path: str = None):
    device = "cuda" if torch.cuda.is_available() else "cpu"
    checkpoints_dir = os.path.join(parent_dir, "checkpoints")

    if checkpoint_path is None:
        checkpoint_path = os.path.join(checkpoints_dir, "12thman_gpt1_best.pt")

    tok_path = os.path.join(checkpoints_dir, "tokenizer.json")
    tokenizer = CricketTokenizer.load(tok_path)

    checkpoint = torch.load(checkpoint_path, map_location=device)
    config = GPTConfig.from_dict(checkpoint["config"])

    model = TwelveManGPT(config)
    model.load_state_dict(checkpoint["model_state_dict"])
    model.to(device)
    model.eval()

    print(f"[Evaluation] Model loaded from {checkpoint_path} (Val Loss: {checkpoint.get('val_loss', 'N/A')})")
    return model, tokenizer, device


def test_generation(prompt: str, max_tokens: int = 50, temperature: float = 0.7):
    model, tokenizer, device = load_model_and_tokenizer()

    input_ids = tokenizer.encode(prompt, add_special_tokens=False)
    input_tensor = torch.tensor([input_ids], dtype=torch.long, device=device)

    generated_ids = model.generate(
        input_tensor,
        max_new_tokens=max_tokens,
        temperature=temperature,
        top_k=30,
        eos_token_id=tokenizer.eos_token_id
    )[0].tolist()

    return tokenizer.decode(generated_ids, skip_special_tokens=True)


if __name__ == "__main__":
    prompt = sys.argv[1] if len(sys.argv) > 1 else "What is Leg Before Wicket (LBW)?"
    result = test_generation(prompt)
    print(f"\nPrompt: {prompt}")
    print(f"Output: {result}")
