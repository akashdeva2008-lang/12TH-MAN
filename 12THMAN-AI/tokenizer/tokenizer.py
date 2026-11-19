"""
12THMAN GPT-1 Custom BPE (Byte Pair Encoding) Tokenizer
Built from scratch without using any pretrained models or libraries.
"""

import json
import os
import re
from typing import List, Dict, Tuple, Optional


class CricketTokenizer:
    """
    Custom Byte-Pair Encoding (BPE) Tokenizer for 12THMAN GPT-1.
    Trained strictly from scratch on cricket domain corpora.
    """

    SPECIAL_TOKENS = [
        "<pad>",
        "<unk>",
        "<bos>",
        "<eos>",
        "<user>",
        "<assistant>",
        "<system>"
    ]

    def __init__(self, vocab_size: int = 2048):
        self.target_vocab_size = vocab_size
        self.vocab: Dict[str, int] = {}
        self.id_to_token: Dict[int, str] = {}
        self.merges: List[Tuple[str, str]] = []

        # Special token IDs
        self.pad_token_id = 0
        self.unk_token_id = 1
        self.bos_token_id = 2
        self.eos_token_id = 3
        self.user_token_id = 4
        self.assistant_token_id = 5
        self.system_token_id = 6

        # Initialize base vocab
        self._init_base_vocab()

    def _init_base_vocab(self):
        self.vocab = {}
        for idx, token in enumerate(self.SPECIAL_TOKENS):
            self.vocab[token] = idx

        # Add standard ASCII characters (printable, space, punctuation, digits, letters)
        for i in range(32, 127):
            char = chr(i)
            if char not in self.vocab:
                self.vocab[char] = len(self.vocab)

        # Common whitespaces and symbols
        for char in ["\n", "\t", "\r", "🏏", "★", "•", "✓", "—"]:
            if char not in self.vocab:
                self.vocab[char] = len(self.vocab)

        self.id_to_token = {v: k for k, v in self.vocab.items()}

    def _get_stats(self, words: Dict[Tuple[str, ...], int]) -> Dict[Tuple[str, str], int]:
        pairs: Dict[Tuple[str, str], int] = {}
        for word, freq in words.items():
            for i in range(len(word) - 1):
                pair = (word[i], word[i + 1])
                pairs[pair] = pairs.get(pair, 0) + freq
        return pairs

    def _merge_vocab(self, pair: Tuple[str, str], words: Dict[Tuple[str, ...], int]) -> Dict[Tuple[str, ...], int]:
        new_words: Dict[Tuple[str, ...], int] = {}
        bigram = pair
        for word, freq in words.items():
            new_word = []
            i = 0
            while i < len(word):
                if i < len(word) - 1 and (word[i], word[i + 1]) == bigram:
                    new_word.append(word[i] + word[i + 1])
                    i += 2
                else:
                    new_word.append(word[i])
                    i += 1
            new_words[tuple(new_word)] = freq
        return new_words

    def train(self, texts: List[str], max_vocab_size: Optional[int] = None):
        """
        Train BPE merges on the provided cricket text dataset.
        """
        target_size = max_vocab_size or self.target_vocab_size
        print(f"[Tokenizer] Starting training on {len(texts)} documents. Target vocab: {target_size}")

        # Pre-tokenize by whitespace and punctuation into word tokens
        word_freqs: Dict[Tuple[str, ...], int] = {}
        for text in texts:
            # Split words while preserving punctuation
            tokens = re.findall(r"\w+|[^\w\s]|\s+", text)
            for token in tokens:
                if not token:
                    continue
                # Represent token as tuple of characters
                char_tuple = tuple(list(token))
                word_freqs[char_tuple] = word_freqs.get(char_tuple, 0) + 1

        self.merges = []
        num_merges = target_size - len(self.vocab)

        for step in range(max(0, num_merges)):
            pairs = self._get_stats(word_freqs)
            if not pairs:
                break
            best_pair = max(pairs, key=pairs.get)
            if pairs[best_pair] < 2:
                # Pair occurs too infrequently to be meaningful
                break

            word_freqs = self._merge_vocab(best_pair, word_freqs)
            new_token = best_pair[0] + best_pair[1]
            if new_token not in self.vocab:
                self.vocab[new_token] = len(self.vocab)
                self.id_to_token[self.vocab[new_token]] = new_token
                self.merges.append(best_pair)

            if len(self.vocab) >= target_size:
                break

        print(f"[Tokenizer] Training complete. Vocabulary size: {len(self.vocab)}, Merges learned: {len(self.merges)}")

    def encode(self, text: str, add_special_tokens: bool = False) -> List[int]:
        """
        Tokenize raw string into list of token IDs.
        """
        if not text:
            return []

        tokens: List[int] = []
        if add_special_tokens:
            tokens.append(self.bos_token_id)

        # Extract words and tokens
        raw_words = re.findall(r"\w+|[^\w\s]|\s+", text)
        for word in raw_words:
            if word in self.vocab:
                tokens.append(self.vocab[word])
                continue

            # Break into characters and apply merges
            word_chars = list(word)
            for pair in self.merges:
                i = 0
                new_chars = []
                while i < len(word_chars):
                    if i < len(word_chars) - 1 and word_chars[i] == pair[0] and word_chars[i + 1] == pair[1]:
                        new_chars.append(pair[0] + pair[1])
                        i += 2
                    else:
                        new_chars.append(word_chars[i])
                        i += 1
                word_chars = new_chars

            for sub in word_chars:
                token_id = self.vocab.get(sub, self.unk_token_id)
                tokens.append(token_id)

        if add_special_tokens:
            tokens.append(self.eos_token_id)

        return tokens

    def decode(self, token_ids: List[int], skip_special_tokens: bool = True) -> str:
        """
        Decode token IDs back into string.
        """
        out_tokens = []
        for tid in token_ids:
            if skip_special_tokens and tid in [
                self.pad_token_id,
                self.bos_token_id,
                self.eos_token_id,
                self.user_token_id,
                self.assistant_token_id,
                self.system_token_id
            ]:
                continue
            token_str = self.id_to_token.get(tid, "")
            out_tokens.append(token_str)
        return "".join(out_tokens)

    def save(self, file_path: str):
        """
        Save vocabulary and merges to JSON file.
        """
        os.makedirs(os.path.dirname(file_path), exist_ok=True)
        data = {
            "vocab": self.vocab,
            "merges": [f"{p[0]} {p[1]}" for p in self.merges],
            "special_tokens": {
                "pad": self.pad_token_id,
                "unk": self.unk_token_id,
                "bos": self.bos_token_id,
                "eos": self.eos_token_id,
                "user": self.user_token_id,
                "assistant": self.assistant_token_id,
                "system": self.system_token_id
            }
        }
        with open(file_path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        print(f"[Tokenizer] Saved tokenizer to {file_path}")

    @classmethod
    def load(cls, file_path: str) -> "CricketTokenizer":
        """
        Load tokenizer from JSON file.
        """
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)

        tokenizer = cls(vocab_size=len(data["vocab"]))
        tokenizer.vocab = {k: int(v) for k, v in data["vocab"].items()}
        tokenizer.id_to_token = {int(v): k for k, v in tokenizer.vocab.items()}
        tokenizer.merges = [tuple(p.split(" ", 1)) for p in data.get("merges", []) if " " in p]

        specials = data.get("special_tokens", {})
        tokenizer.pad_token_id = specials.get("pad", 0)
        tokenizer.unk_token_id = specials.get("unk", 1)
        tokenizer.bos_token_id = specials.get("bos", 2)
        tokenizer.eos_token_id = specials.get("eos", 3)
        tokenizer.user_token_id = specials.get("user", 4)
        tokenizer.assistant_token_id = specials.get("assistant", 5)
        tokenizer.system_token_id = specials.get("system", 6)

        return tokenizer

    @property
    def vocab_size(self) -> int:
        return len(self.vocab)
