# 10. Tries & Bit Manipulation

---

## 10.1 Trie (Prefix Tree)

**Signals**
- "Starts with / prefix", "autocomplete / search suggestions", "replace words with their root", "word dictionary with `.` wildcard".
- Searching **many words at once** in a grid (Word Search II).
- "**Maximum XOR** of two numbers" → a binary trie over the bits.

**Intuition**
Words that share a prefix share a path from the root, so a lookup costs O(word length) no matter how many words are stored.
Each node has up to 26 children plus an `end` flag (and optionally a count, or the word itself).
**XOR trie:** insert numbers bit by bit from the most significant bit. To maximise `x ^ y`, at each bit go toward the **opposite** bit when that child exists.

**C++ template**
```cpp
struct Trie {
    struct Node { Node* ch[26] = {}; bool end = false; int pass = 0; };
    Node* root = new Node();

    void insert(const string& w) {
        Node* cur = root;
        for (char c : w) {
            if (!cur->ch[c - 'a']) cur->ch[c - 'a'] = new Node();
            cur = cur->ch[c - 'a']; cur->pass++;           // pass = number of words with this prefix
        }
        cur->end = true;
    }
    Node* walk(const string& s) {
        Node* cur = root;
        for (char c : s) { cur = cur->ch[c - 'a']; if (!cur) return nullptr; }
        return cur;
    }
    bool search(const string& w)     { Node* n = walk(w); return n && n->end; }
    bool startsWith(const string& p) { return walk(p) != nullptr; }
};

// Wildcard search ('.' matches any letter): DFS over all children
bool searchDot(Trie::Node* n, const string& w, int i) {
    if (!n) return false;
    if (i == (int)w.size()) return n->end;
    if (w[i] != '.') return searchDot(n->ch[w[i] - 'a'], w, i + 1);
    for (auto c : n->ch) if (searchDot(c, w, i + 1)) return true;
    return false;
}

// Maximum XOR of two numbers
int findMaximumXOR(vector<int>& a) {
    struct B { B* c[2] = {}; };
    B* root = new B(); int best = 0;
    for (int x : a) {
        B *ins = root, *q = root; int cur = 0;
        for (int b = 31; b >= 0; b--) {
            int bit = (x >> b) & 1;
            if (!ins->c[bit]) ins->c[bit] = new B();
            ins = ins->c[bit];
            if (q->c[!bit]) { cur |= (1 << b); q = q->c[!bit]; }
            else q = q->c[bit];
        }
        best = max(best, cur);
    }
    return best;
}
```

**Complexity:** O(L) per operation, O(total characters · 26) space.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [14. Longest Common Prefix](https://leetcode.com/problems/longest-common-prefix/) | Easy |
| 2 | [208. Implement Trie (Prefix Tree)](https://leetcode.com/problems/implement-trie-prefix-tree/) | Medium |
| 3 | [211. Design Add and Search Words Data Structure](https://leetcode.com/problems/design-add-and-search-words-data-structure/) | Medium |
| 4 | [648. Replace Words](https://leetcode.com/problems/replace-words/) | Medium |
| 5 | [677. Map Sum Pairs](https://leetcode.com/problems/map-sum-pairs/) | Medium |
| 6 | [720. Longest Word in Dictionary](https://leetcode.com/problems/longest-word-in-dictionary/) | Medium |
| 7 | [1268. Search Suggestions System](https://leetcode.com/problems/search-suggestions-system/) | Medium |
| 8 | [421. Maximum XOR of Two Numbers in an Array](https://leetcode.com/problems/maximum-xor-of-two-numbers-in-an-array/) | Medium |
| 9 | [2416. Sum of Prefix Scores of Strings](https://leetcode.com/problems/sum-of-prefix-scores-of-strings/) | Hard |
| 10 | [1707. Maximum XOR With an Element From Array](https://leetcode.com/problems/maximum-xor-with-an-element-from-array/) | Hard |

---

## 10.2 Bit Manipulation

**Signals**
- "Every element appears twice except one", "count set bits", "power of two", "add without `+`".
- Representing **subsets as integers** (n ≤ 20) for enumeration or bitmask DP.
- O(1) space required when the input is numbers.

**Intuition: the identities that matter**

| Trick | Meaning |
|-------|---------|
| `x ^ x = 0`, `x ^ 0 = x` | XOR cancels pairs, so XOR everything to find the single number |
| `x & (x - 1)` | removes the lowest set bit (count bits, power-of-two check) |
| `x & -x` | isolates the lowest set bit (splits numbers into two groups in Single Number III) |
| `(x >> i) & 1` | reads bit i |
| `x \| (1 << i)`, `x & ~(1 << i)`, `x ^ (1 << i)` | set, clear, toggle bit i |
| `__builtin_popcount(x)` | number of set bits (`__builtin_popcountll` for 64-bit) |
| `a + b = (a ^ b) + ((a & b) << 1)` | addition without `+` |
| Count bit i over all numbers, mod 3 | the single number when the others appear three times |
| `for (s = mask; s; s = (s - 1) & mask)` | enumerate all non-empty submasks of `mask` (the loop stops before `s = 0`, so handle the empty set separately) |

**C++ template**
```cpp
int singleNumber(vector<int>& a) { int x = 0; for (int v : a) x ^= v; return x; }

int singleNumberII(vector<int>& a) {               // the others appear 3 times
    int res = 0;
    for (int b = 0; b < 32; b++) {
        int cnt = 0;
        for (int v : a) cnt += (v >> b) & 1;
        if (cnt % 3) res |= (1 << b);
    }
    return res;
}

vector<int> singleNumberIII(vector<int>& a) {      // two unique numbers
    unsigned x = 0; for (int v : a) x ^= v;
    unsigned low = x & -x;                         // a bit where the two numbers differ
    int p = 0, q = 0;
    for (int v : a) (v & low ? p : q) ^= v;
    return {p, q};
}

vector<int> countBits(int n) {                     // DP over bits
    vector<int> dp(n + 1, 0);
    for (int i = 1; i <= n; i++) dp[i] = dp[i >> 1] + (i & 1);
    return dp;
}

bool isPowerOfTwo(int n) { return n > 0 && (n & (n - 1)) == 0; }

int rangeBitwiseAnd(int l, int r) {                // common prefix of l and r
    int shift = 0;
    while (l != r) { l >>= 1; r >>= 1; shift++; }
    return l << shift;
}
```

**Complexity:** O(n) or O(32n), O(1) space.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [136. Single Number](https://leetcode.com/problems/single-number/) | Easy |
| 2 | [191. Number of 1 Bits](https://leetcode.com/problems/number-of-1-bits/) | Easy |
| 3 | [338. Counting Bits](https://leetcode.com/problems/counting-bits/) | Easy |
| 4 | [190. Reverse Bits](https://leetcode.com/problems/reverse-bits/) | Easy |
| 5 | [231. Power of Two](https://leetcode.com/problems/power-of-two/) | Easy |
| 6 | [137. Single Number II](https://leetcode.com/problems/single-number-ii/) | Medium |
| 7 | [260. Single Number III](https://leetcode.com/problems/single-number-iii/) | Medium |
| 8 | [371. Sum of Two Integers](https://leetcode.com/problems/sum-of-two-integers/) | Medium |
| 9 | [201. Bitwise AND of Numbers Range](https://leetcode.com/problems/bitwise-and-of-numbers-range/) | Medium |
| 10 | [1318. Minimum Flips to Make a OR b Equal to c](https://leetcode.com/problems/minimum-flips-to-make-a-or-b-equal-to-c/) | Medium |
