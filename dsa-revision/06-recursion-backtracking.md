# 6. Recursion & Backtracking

**Backtracking = DFS over a decision tree.** At each level you make a choice, recurse, then **undo** the
choice so the next branch starts from a clean state.

```
void backtrack(state) {
    if (state is a complete answer) { record it; return; }
    for (each choice available in this state) {
        if (choice is invalid) continue;     // pruning
        make(choice);
        backtrack(next state);
        undo(choice);                         // this undo step is what makes it backtracking
    }
}
```

**Questions to answer before coding**
1. What does one **level** of recursion decide? (one element: take it or skip it; or one position: which value goes here)
2. When is the path a **complete answer**?
3. How do I **avoid duplicates**? (sort and skip equal siblings, use a start index, use a `used[]` array)
4. What can I **prune** early? (the remaining sum is already negative, the board is invalid)

**Recognising backtracking:** "return **all** possible...", "generate every...", and small constraints (n ≤ 15–20).
If the question only asks for a **count** or a **min/max**, it is often DP instead (backtracking with memoisation).

---

## 6.1 Subsets (Include / Exclude)

**Signals**
- "All subsets / power set", "every combination of letter cases", "partition into k equal subsets".
- For each element the decision is binary: **take it or leave it**.

**Intuition**
n elements with two choices each gives 2ⁿ leaves. Two equivalent ways to write it:
- **Binary-choice recursion:** at index i, recurse with and without `a[i]`.
- **Loop-from-start recursion:** every node of the tree is a valid subset; loop `j` from `start` to `n-1`, add `a[j]`, recurse with `j+1`.

**Duplicates (Subsets II):** sort, then skip `a[j]` when `j > start && a[j] == a[j-1]`, so equal values are only picked in order at each level.

**C++ template**
```cpp
vector<vector<int>> res; vector<int> path;

void subsets(vector<int>& a, int start) {          // call after sort(a) when a has duplicates
    res.push_back(path);                           // every node is an answer
    for (int j = start; j < (int)a.size(); j++) {
        if (j > start && a[j] == a[j - 1]) continue;   // skip duplicates (Subsets II)
        path.push_back(a[j]);
        subsets(a, j + 1);
        path.pop_back();
    }
}

// Bitmask enumeration (n <= 20)
for (int mask = 0; mask < (1 << n); mask++) {
    vector<int> cur;
    for (int i = 0; i < n; i++) if (mask >> i & 1) cur.push_back(a[i]);
}

// Generate Parentheses: choices limited by counts
void gen(int open, int close, int n, string& cur, vector<string>& out) {
    if ((int)cur.size() == 2 * n) { out.push_back(cur); return; }
    if (open < n)     { cur.push_back('('); gen(open + 1, close, n, cur, out); cur.pop_back(); }
    if (close < open) { cur.push_back(')'); gen(open, close + 1, n, cur, out); cur.pop_back(); }
}
```

**Complexity:** O(n · 2ⁿ).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [1863. Sum of All Subset XOR Totals](https://leetcode.com/problems/sum-of-all-subset-xor-totals/) | Easy |
| 2 | [78. Subsets](https://leetcode.com/problems/subsets/) | Medium |
| 3 | [90. Subsets II](https://leetcode.com/problems/subsets-ii/) | Medium |
| 4 | [784. Letter Case Permutation](https://leetcode.com/problems/letter-case-permutation/) | Medium |
| 5 | [17. Letter Combinations of a Phone Number](https://leetcode.com/problems/letter-combinations-of-a-phone-number/) | Medium |
| 6 | [22. Generate Parentheses](https://leetcode.com/problems/generate-parentheses/) | Medium |
| 7 | [2044. Count Number of Maximum Bitwise-OR Subsets](https://leetcode.com/problems/count-number-of-maximum-bitwise-or-subsets/) | Medium |
| 8 | [1239. Maximum Length of a Concatenated String with Unique Characters](https://leetcode.com/problems/maximum-length-of-a-concatenated-string-with-unique-characters/) | Medium |
| 9 | [473. Matchsticks to Square](https://leetcode.com/problems/matchsticks-to-square/) | Medium |
| 10 | [698. Partition to K Equal Sum Subsets](https://leetcode.com/problems/partition-to-k-equal-sum-subsets/) | Medium |

---

## 6.2 Permutations (Ordering Matters)

**Signals**
- "All permutations / arrangements / orderings", "kth permutation", "beautiful arrangement".

**Intuition**
Each level fills **one position** with any element not used yet, so you need a `used[]` array (or in-place swapping).
That gives n! leaves.
**Duplicates (Permutations II):** sort, then skip `a[i]` if `a[i] == a[i-1] && !used[i-1]`, so equal values
are always used in their original order.

**C++ template**
```cpp
vector<vector<int>> res; vector<int> path; vector<bool> used;

void permute(vector<int>& a) {                     // sort(a) first if there are duplicates
    if (path.size() == a.size()) { res.push_back(path); return; }
    for (int i = 0; i < (int)a.size(); i++) {
        if (used[i]) continue;
        if (i > 0 && a[i] == a[i - 1] && !used[i - 1]) continue;   // Permutations II
        used[i] = true; path.push_back(a[i]);
        permute(a);
        path.pop_back(); used[i] = false;
    }
}

// Swap-based version (no duplicates)
void permSwap(vector<int>& a, int pos) {
    if (pos == (int)a.size()) { res.push_back(a); return; }
    for (int i = pos; i < (int)a.size(); i++) {
        swap(a[pos], a[i]);
        permSwap(a, pos + 1);
        swap(a[pos], a[i]);
    }
}
```

**Complexity:** O(n · n!).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [46. Permutations](https://leetcode.com/problems/permutations/) | Medium |
| 2 | [47. Permutations II](https://leetcode.com/problems/permutations-ii/) | Medium |
| 3 | [1079. Letter Tile Possibilities](https://leetcode.com/problems/letter-tile-possibilities/) | Medium |
| 4 | [526. Beautiful Arrangement](https://leetcode.com/problems/beautiful-arrangement/) | Medium |
| 5 | [89. Gray Code](https://leetcode.com/problems/gray-code/) | Medium |
| 6 | [1415. The k-th Lexicographical String of All Happy Strings of Length n](https://leetcode.com/problems/the-k-th-lexicographical-string-of-all-happy-strings-of-length-n/) | Medium |
| 7 | [1980. Find Unique Binary String](https://leetcode.com/problems/find-unique-binary-string/) | Medium |
| 8 | [1718. Construct the Lexicographically Largest Valid Sequence](https://leetcode.com/problems/construct-the-lexicographically-largest-valid-sequence/) | Medium |
| 9 | [60. Permutation Sequence](https://leetcode.com/problems/permutation-sequence/) | Hard |
| 10 | [996. Number of Squareful Arrays](https://leetcode.com/problems/number-of-squareful-arrays/) | Hard |

---

## 6.3 Combinations, Combination Sum & Partitioning

**Signals**
- "Choose k numbers", "all combinations that sum to target", "partition the string into palindromes / valid IP parts / dictionary words".

**Intuition**
Same shape as subsets, but with a **target** and a **start index** so you never produce the same
combination in a different order.
- **Reuse allowed** (Combination Sum): recurse with `i` (stay on the same element).
- **No reuse** (Combination Sum II): recurse with `i+1` and skip duplicate siblings.
- **Partitioning:** the "choice" is where the next piece ends: try `s[start..end]` for each `end`, and recurse on `end+1` if the piece is valid.

**Prune:** sort the input, then `break` as soon as `a[i] > remaining`.

**C++ template**
```cpp
vector<vector<int>> res; vector<int> path;

void combSum(vector<int>& a, int start, int remain, bool reuse) {   // a sorted
    if (remain == 0) { res.push_back(path); return; }
    for (int i = start; i < (int)a.size(); i++) {
        if (a[i] > remain) break;                                    // pruning
        if (!reuse && i > start && a[i] == a[i - 1]) continue;       // Combination Sum II
        path.push_back(a[i]);
        combSum(a, reuse ? i : i + 1, remain - a[i], reuse);
        path.pop_back();
    }
}

// Palindrome Partitioning
vector<vector<string>> out; vector<string> parts;
bool isPal(const string& s, int l, int r) { while (l < r) if (s[l++] != s[r--]) return false; return true; }
void partition(const string& s, int start) {
    if (start == (int)s.size()) { out.push_back(parts); return; }
    for (int end = start; end < (int)s.size(); end++) {
        if (!isPal(s, start, end)) continue;
        parts.push_back(s.substr(start, end - start + 1));
        partition(s, end + 1);
        parts.pop_back();
    }
}
```

**Complexity:** exponential; the exact bound depends on how much pruning removes.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [401. Binary Watch](https://leetcode.com/problems/binary-watch/) | Easy |
| 2 | [77. Combinations](https://leetcode.com/problems/combinations/) | Medium |
| 3 | [39. Combination Sum](https://leetcode.com/problems/combination-sum/) | Medium |
| 4 | [40. Combination Sum II](https://leetcode.com/problems/combination-sum-ii/) | Medium |
| 5 | [216. Combination Sum III](https://leetcode.com/problems/combination-sum-iii/) | Medium |
| 6 | [131. Palindrome Partitioning](https://leetcode.com/problems/palindrome-partitioning/) | Medium |
| 7 | [93. Restore IP Addresses](https://leetcode.com/problems/restore-ip-addresses/) | Medium |
| 8 | [1286. Iterator for Combination](https://leetcode.com/problems/iterator-for-combination/) | Medium |
| 9 | [140. Word Break II](https://leetcode.com/problems/word-break-ii/) | Hard |
| 10 | [282. Expression Add Operators](https://leetcode.com/problems/expression-add-operators/) | Hard |

---

## 6.4 Grid & Constraint-Satisfaction Backtracking

**Signals**
- "Find a word in a grid", "N-Queens", "Sudoku solver", "visit every empty cell exactly once".
- Constraints that rule out choices as you place things.

**Intuition**
Move through cells (or rows, for N-Queens) and place or visit something. Mark it **visited / used**, recurse,
then **unmark** it. Fast constraint checks are key: N-Queens uses `cols`, `diag (r+c)` and `antiDiag (r-c+n)` arrays
so each check is O(1). Sudoku uses `row[9][10]`, `col[9][10]` and `box[9][10]`.

**C++ template**
```cpp
// Word Search
bool dfs(vector<vector<char>>& b, const string& w, int i, int r, int c) {
    if (i == (int)w.size()) return true;
    if (r < 0 || c < 0 || r >= (int)b.size() || c >= (int)b[0].size() || b[r][c] != w[i]) return false;
    char tmp = b[r][c]; b[r][c] = '#';                 // mark visited
    bool found = dfs(b, w, i + 1, r + 1, c) || dfs(b, w, i + 1, r - 1, c) ||
                 dfs(b, w, i + 1, r, c + 1) || dfs(b, w, i + 1, r, c - 1);
    b[r][c] = tmp;                                      // unmark
    return found;
}

// N-Queens
int n; vector<string> board; vector<vector<string>> sols;
vector<bool> col, d1, d2;                               // sizes n, 2n, 2n
void solve(int r) {
    if (r == n) { sols.push_back(board); return; }
    for (int c = 0; c < n; c++) {
        if (col[c] || d1[r + c] || d2[r - c + n]) continue;
        col[c] = d1[r + c] = d2[r - c + n] = true; board[r][c] = 'Q';
        solve(r + 1);
        col[c] = d1[r + c] = d2[r - c + n] = false; board[r][c] = '.';
    }
}
```

**Complexity:** exponential (N-Queens is about O(n!)).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [79. Word Search](https://leetcode.com/problems/word-search/) | Medium |
| 2 | [1219. Path with Maximum Gold](https://leetcode.com/problems/path-with-maximum-gold/) | Medium |
| 3 | [2597. The Number of Beautiful Subsets](https://leetcode.com/problems/the-number-of-beautiful-subsets/) | Medium |
| 4 | [51. N-Queens](https://leetcode.com/problems/n-queens/) | Hard |
| 5 | [52. N-Queens II](https://leetcode.com/problems/n-queens-ii/) | Hard |
| 6 | [37. Sudoku Solver](https://leetcode.com/problems/sudoku-solver/) | Hard |
| 7 | [980. Unique Paths III](https://leetcode.com/problems/unique-paths-iii/) | Hard |
| 8 | [212. Word Search II](https://leetcode.com/problems/word-search-ii/) (backtracking + Trie) | Hard |
| 9 | [301. Remove Invalid Parentheses](https://leetcode.com/problems/remove-invalid-parentheses/) | Hard |
| 10 | [1240. Tiling a Rectangle with the Fewest Squares](https://leetcode.com/problems/tiling-a-rectangle-with-the-fewest-squares/) | Hard |
