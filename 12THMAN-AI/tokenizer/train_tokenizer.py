"""
Train the 12THMAN Custom BPE Tokenizer on the cricket domain dataset.
"""

import os
import sys

# Add parent directory to path so imports work
current_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.dirname(current_dir)
if parent_dir not in sys.path:
    sys.path.insert(0, parent_dir)

try:
    from tokenizer.tokenizer import CricketTokenizer
except ImportError:
    from tokenizer import CricketTokenizer


def train_and_save_tokenizer():
    dataset_path = os.path.join(parent_dir, "dataset", "cricket_knowledge.txt")
    if not os.path.exists(dataset_path):
        raise FileNotFoundError(f"Dataset not found at {dataset_path}")

    with open(dataset_path, "r", encoding="utf-8") as f:
        corpus_text = f.read()

    # Split into paragraphs/articles
    texts = [t.strip() for t in corpus_text.split("\n\n") if t.strip()]

    # Initialize tokenizer with target vocabulary size
    tokenizer = CricketTokenizer(vocab_size=1024)
    tokenizer.train(texts, max_vocab_size=1024)

    # Save to tokenizer directory
    save_path = os.path.join(current_dir, "tokenizer.json")
    tokenizer.save(save_path)

    # Also save to checkpoints directory for backend loading
    checkpoint_tok_path = os.path.join(parent_dir, "checkpoints", "tokenizer.json")
    tokenizer.save(checkpoint_tok_path)

    # Test encoding / decoding
    test_phrase = "Cover Drive through the covers! LBW out by Yorker."
    encoded = tokenizer.encode(test_phrase, add_special_tokens=True)
    decoded = tokenizer.decode(encoded, skip_special_tokens=True)

    print(f"\n[Tokenizer Test]")
    print(f"Original: {test_phrase}")
    print(f"Token IDs ({len(encoded)} tokens): {encoded}")
    print(f"Decoded:  {decoded}")
    assert test_phrase == decoded, f"Mismatch: '{test_phrase}' != '{decoded}'"
    print("Verification: Lossless Tokenizer encoding & decoding confirmed!")


if __name__ == "__main__":
    train_and_save_tokenizer()
