# DSA Revision in C++: Arrays to Dynamic Programming

Revision notes organised by **pattern**. For each pattern you get:

- **Signals:** words or constraints in a question that point to this pattern
- **Intuition:** why the pattern works
- **Approach:** the steps to follow once you've spotted it
- **C++ template:** reusable code
- **Complexity**
- **10 LeetCode practice problems**, ordered roughly Easy → Hard

## Table of Contents

| # | File | Patterns |
|---|------|----------|
| 1 | [Arrays & Hashing](01-arrays-hashing.md) | Hashing / Frequency, Prefix Sum & Difference Array, Kadane, Cyclic Sort, Matrix In-place |
| 2 | [Two Pointers & Sliding Window](02-two-pointers-sliding-window.md) | Opposite Ends, Same Direction (Read/Write), Fixed Window, Variable Window |
| 3 | [Binary Search](03-binary-search.md) | Classic / Boundaries, Rotated & Modified, Binary Search on Answer |
| 4 | [Stack & Queue](04-stack-queue.md) | Stack (Matching / Expression), Monotonic Stack, Queue & Monotonic Deque |
| 5 | [Linked List](05-linked-list.md) | In-place Reversal, Fast & Slow Pointers, Dummy Node / Merge / Design |
| 6 | [Recursion & Backtracking](06-recursion-backtracking.md) | Subsets, Permutations, Combinations / Partitioning, Grid & Constraint Backtracking |
| 7 | [Trees](07-trees.md) | DFS (Bottom-up), BFS Level Order, Paths & LCA (Top-down), BST, Construction & Serialization |
| 8 | [Heaps, Greedy & Intervals](08-heaps-greedy-intervals.md) | Top-K, Two Heaps / Scheduling, K-way Merge, Intervals, Greedy |
| 9 | [Graphs](09-graphs.md) | DFS/BFS Components, Multi-source BFS, Topological Sort, Union-Find, Weighted Shortest Path, MST & Advanced |
| 10 | [Tries & Bit Manipulation](10-tries-bits.md) | Trie, Bit Manipulation |
| 11 | [Dynamic Programming](11-dynamic-programming.md) | 1D Linear, 0/1 Knapsack, Unbounded Knapsack, Grid, Two Strings (LCS), LIS, Palindromes, Interval (MCM), State Machine (Stocks), Trees, Bitmask, Digit DP |

---

## Master Cheat Sheet: Spotting the Pattern

Read the question, then check the **constraints** and **keywords** against this table.

### From constraints (n = input size)

| n up to | Target complexity | Likely patterns |
|---------|-------------------|-----------------|
| ≤ 10–12 | O(n!) / O(2ⁿ·n) | Permutations, backtracking |
| ≤ 20 | O(2ⁿ) | Subsets, bitmask DP |
| ≤ 100–500 | O(n³) | Interval DP, Floyd–Warshall |
| ≤ 5,000 | O(n²) | 2D DP, LIS O(n²), nested loops |
| ≤ 10⁵–10⁶ | O(n log n) / O(n) | Sorting, binary search, heap, two pointers, sliding window, prefix sum, monotonic stack, greedy |
| ≥ 10⁹ | O(log n) / O(1) | Binary search on answer, math, bits |

### From keywords

| If you see... | Think... |
|---------------|----------|
| "pair / triplet with sum", sorted array | Two pointers (opposite ends) |
| "find whether X exists", "count frequency", "duplicates" | Hash map / set |
| "contiguous subarray / substring" + longest / shortest / at most K | Sliding window |
| "subarray sum equals K", negatives allowed | Prefix sum + hash map |
| "range sum queries", "add v to range [l, r] many times" | Prefix sum / difference array |
| "maximum subarray sum" | Kadane |
| numbers in range `[1..n]`, missing / duplicate | Cyclic sort / index marking |
| sorted / rotated array, "find in O(log n)" | Binary search |
| "minimise the maximum" / "maximise the minimum" / "minimum capacity / speed / days" | Binary search on answer |
| "next greater / smaller", "span", "histogram" | Monotonic stack |
| "max / min in every window of size k" | Monotonic deque |
| matching brackets, nested structure, undo | Stack |
| linked list cycle, middle, kth from end | Fast & slow pointers |
| reverse part of a list | In-place reversal |
| "all combinations / subsets / permutations", "generate all" | Backtracking |
| tree: height, diameter, balanced, max path | DFS post-order (bottom-up) |
| tree: level-wise, right view, min depth | BFS |
| BST property, kth smallest, sorted order | Inorder traversal |
| "top / kth largest / smallest / most frequent" | Heap (size k) |
| "running median" | Two heaps |
| merge k sorted lists or arrays | K-way merge (min-heap) |
| overlapping intervals, meetings, rooms | Sort + sweep / intervals |
| locally optimal choice works (provable by exchange argument) | Greedy |
| grid islands, connected regions | DFS / BFS flood fill |
| shortest path, unweighted | BFS |
| shortest path, weighted non-negative | Dijkstra |
| dependencies, prerequisites, ordering | Topological sort |
| "are they connected?", dynamic grouping, redundant edge | Union-Find |
| connect all points at minimum cost | MST (Kruskal / Prim) |
| prefix search, autocomplete, word dictionary | Trie |
| maximum XOR | Bitwise trie |
| "appears once while others appear twice", toggling, subsets as ints | Bit manipulation |
| "count number of ways", "min / max cost", "is it possible", overlapping choices | Dynamic programming |
| choose / skip items with capacity | Knapsack DP |
| two strings: match / edit / common | 2D string DP (LCS family) |
| increasing subsequence, chains, nesting | LIS DP |
| answer for range `[i..j]` depends on splitting it | Interval DP |
| states like hold / sold / cooldown | State machine DP |
| count numbers in `[L, R]` with a digit property | Digit DP |

---

## How to Use These Notes

1. **Pass 1 (learn):** read the intuition, type the template from memory, then solve problems 1–4 for each pattern.
2. **Pass 2 (apply):** solve problems 5–8 without looking at the pattern name. Before you write code, say which pattern applies.
3. **Pass 3 (master):** solve problems 9–10 (Hard). Re-solve any problem you got wrong after 3 days, then again after 7 days.
4. **Before interviews:** re-read only the *Signals* and *Templates* sections in each file.

### C++ Setup Used in the Templates

```cpp
#include <bits/stdc++.h>
using namespace std;
using ll = long long;
using pii = pair<int,int>;
```
