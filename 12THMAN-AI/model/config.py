"""
12THMAN GPT-1 Model Configuration
"""

from dataclasses import dataclass
import json


@dataclass
class GPTConfig:
    vocab_size: int = 1024
    block_size: int = 128       # Context window (tokens)
    n_layer: int = 4            # Number of Transformer decoder layers
    n_head: int = 4             # Number of attention heads
    n_embd: int = 128           # Embedding dimension
    dropout: float = 0.1        # Dropout rate
    bias: bool = False          # Use bias in Linears and LayerNorms

    def to_dict(self):
        return {
            "vocab_size": self.vocab_size,
            "block_size": self.block_size,
            "n_layer": self.n_layer,
            "n_head": self.n_head,
            "n_embd": self.n_embd,
            "dropout": self.dropout,
            "bias": self.bias
        }

    @classmethod
    def from_dict(cls, d):
        return cls(**d)

    def save(self, path: str):
        with open(path, "w", encoding="utf-8") as f:
            json.dump(self.to_dict(), f, indent=2)

    @classmethod
    def load(cls, path: str):
        with open(path, "r", encoding="utf-8") as f:
            return cls.from_dict(json.load(f))
