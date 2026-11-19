"""
12THMAN GPT-1 Dataset Loader
Loads, tokenizes, and splits cricket dataset into train/val tensors.
"""

import os
import json
import torch
from typing import Tuple
from tokenizer.tokenizer import CricketTokenizer


class CricketDataset:
    def __init__(self, data_dir: str, tokenizer: CricketTokenizer, block_size: int = 128):
        self.tokenizer = tokenizer
        self.block_size = block_size

        raw_txt_path = os.path.join(data_dir, "cricket_knowledge.txt")
        jsonl_path = os.path.join(data_dir, "cricket_qa.jsonl")

        full_text = []

        # 1. Load domain knowledge
        if os.path.exists(raw_txt_path):
            with open(raw_txt_path, "r", encoding="utf-8") as f:
                full_text.append(f.read())

        # 2. Load QA conversations
        if os.path.exists(jsonl_path):
            with open(jsonl_path, "r", encoding="utf-8") as f:
                for line in f:
                    if line.strip():
                        qa = json.loads(line)
                        conv = f"<user> {qa['user']} <assistant> {qa['assistant']} <eos>\n"
                        full_text.append(conv)

        merged_corpus = "\n\n".join(full_text)
        print(f"[Dataset] Loaded {len(merged_corpus)} characters of cricket text.")

        # Tokenize entire corpus
        token_ids = self.tokenizer.encode(merged_corpus, add_special_tokens=True)
        print(f"[Dataset] Total encoded tokens: {len(token_ids)}")

        # Train / Validation Split (90% Train, 10% Validation)
        n = int(0.9 * len(token_ids))
        self.train_data = torch.tensor(token_ids[:n], dtype=torch.long)
        self.val_data = torch.tensor(token_ids[n:], dtype=torch.long)
        print(f"[Dataset] Split into {len(self.train_data)} train tokens and {len(self.val_data)} val tokens.")

    def get_batch(self, split: str, batch_size: int = 8, device: str = "cpu") -> Tuple[torch.Tensor, torch.Tensor]:
        data = self.train_data if split == "train" else self.val_data
        if len(data) <= self.block_size:
            # Replicate if data is short
            data = data.repeat((self.block_size // len(data)) + 2)

        max_start = len(data) - self.block_size - 1
        ix = torch.randint(0, max_start, (batch_size,))
        x = torch.stack([data[i:i + self.block_size] for i in ix])
        y = torch.stack([data[i + 1:i + self.block_size + 1] for i in ix])

        x = x.to(device)
        y = y.to(device)
        return x, y
