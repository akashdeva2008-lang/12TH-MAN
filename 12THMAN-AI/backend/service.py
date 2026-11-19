"""
Inference Service for 12THMAN GPT-1.
Loads the trained checkpoint and runs autoregressive generation.
"""

import os
import sys
import time
import torch
from typing import Optional, List, Dict

backend_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = os.path.dirname(backend_dir)
if root_dir not in sys.path:
    sys.path.insert(0, root_dir)

from model.config import GPTConfig
from model.model import TwelveManGPT
from tokenizer.tokenizer import CricketTokenizer


class ModelService:
    _instance = None

    def __init__(self):
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.tokenizer = None
        self.model = None
        self.config = None
        self.val_loss = 0.0
        self.checkpoint_name = "12thman_gpt1_best.pt"
        self._load()

    @classmethod
    def get_instance(cls):
        if cls._instance is None:
            cls._instance = ModelService()
        return cls._instance

    def _load(self):
        checkpoints_dir = os.path.join(root_dir, "checkpoints")
        tok_path = os.path.join(checkpoints_dir, "tokenizer.json")
        if not os.path.exists(tok_path):
            tok_path = os.path.join(root_dir, "tokenizer", "tokenizer.json")

        self.tokenizer = CricketTokenizer.load(tok_path)

        config_path = os.path.join(checkpoints_dir, "model_config.json")
        if os.path.exists(config_path):
            self.config = GPTConfig.load(config_path)
        else:
            self.config = GPTConfig(
                vocab_size=self.tokenizer.vocab_size,
                block_size=128,
                n_layer=4,
                n_head=4,
                n_embd=128
            )

        self.model = TwelveManGPT(self.config)

        ckpt_path = os.path.join(checkpoints_dir, self.checkpoint_name)
        if os.path.exists(ckpt_path):
            ckpt = torch.load(ckpt_path, map_location=self.device)
            self.model.load_state_dict(ckpt["model_state_dict"])
            self.val_loss = ckpt.get("val_loss", 0.0)
            print(f"[ModelService] Loaded checkpoint from {ckpt_path} (Val Loss: {self.val_loss:.4f})")
        else:
            print(f"[ModelService] Warning: Checkpoint not found at {ckpt_path}, using initialized weights.")

        self.model.to(self.device)
        self.model.eval()

    def generate(
        self,
        prompt: str,
        max_new_tokens: int = 50,
        temperature: float = 0.7,
        top_k: int = 25
    ) -> (str, int, float):
        start_t = time.time()
        input_ids = self.tokenizer.encode(prompt, add_special_tokens=False)
        if not input_ids:
            input_ids = [self.tokenizer.special_tokens.get("<bos>", 2)]

        input_tensor = torch.tensor([input_ids], dtype=torch.long, device=self.device)
        with torch.no_grad():
            output_tensor = self.model.generate(
                input_tensor,
                max_new_tokens=max_new_tokens,
                temperature=temperature,
                top_k=top_k,
                eos_token_id=self.tokenizer.eos_token_id
            )

        output_ids = output_tensor[0].tolist()
        new_ids = output_ids[len(input_ids):]
        generated_text = self.tokenizer.decode(new_ids, skip_special_tokens=True).strip()

        latency_ms = (time.time() - start_t) * 1000.0
        return generated_text, len(new_ids), latency_ms

    def chat(
        self,
        user_message: str,
        history: Optional[List] = None,
        max_new_tokens: int = 60,
        temperature: float = 0.7,
        top_k: int = 25
    ) -> (str, int, float):
        # Format as cricket Q&A prompt
        formatted_prompt = f"Question: {user_message.strip()}\nAnswer:"

        generated_text, tokens_count, latency = self.generate(
            formatted_prompt,
            max_new_tokens=max_new_tokens,
            temperature=temperature,
            top_k=top_k
        )

        # Stop at subsequent questions if generated
        if "\nQuestion:" in generated_text:
            generated_text = generated_text.split("\nQuestion:")[0].strip()
        if "Question:" in generated_text:
            generated_text = generated_text.split("Question:")[0].strip()

        # If empty or too short, provide a cricket domain response
        if not generated_text:
            generated_text = "In cricket, this is handled according to standard MCC cricket laws and match playing conditions."

        return generated_text, tokens_count, latency

    def get_info(self) -> Dict:
        return {
            "name": "12THMAN GPT-1",
            "type": "Custom Cricket Language Model",
            "architecture": "Decoder-Only Transformer (GPT)",
            "pretrained_weights": False,
            "weights_origin": "Trained from scratch on cricket domain corpus",
            "parameters": self.model.get_num_params(),
            "vocab_size": self.tokenizer.vocab_size,
            "n_layer": self.config.n_layer,
            "n_head": self.config.n_head,
            "n_embd": self.config.n_embd,
            "block_size": self.config.block_size,
            "val_loss": float(self.val_loss),
            "checkpoint_file": self.checkpoint_name
        }
