# 11. Dynamic Programming

**DP = recursion + memoisation.** Use it when a problem has:
1. **Optimal substructure:** the answer can be built from answers to smaller subproblems.
2. **Overlapping subproblems:** the same subproblem comes up again and again in plain recursion.

**Signals:** "count the number of ways", "minimum / maximum cost", "is it possible", "longest / shortest ..."
where **choices at one step affect later steps** (so greedy fails).

### The 5-step framework (use it for every DP problem)

1. **State:** what does `dp[i]` (or `dp[i][j]`) *mean*? Write it as a sentence, e.g. "dp[i] = max money robbing houses 0..i".
2. **Transition:** how does a state depend on smaller states? List the **choices** at this step (take / skip, which coin, where to split).
3. **Base cases:** the smallest states you can answer directly.
4. **Order:** compute states so their dependencies are already filled in (or use recursion with memoisation and don't worry about order).
5. **Answer:** which state, or which max over states, is the final answer?

### How to build the solution

```
Recursion (brute force)  →  + memo (top-down)  →  tabulation (bottom-up)  →  space optimisation
```
Start with the recursive version: it is the easiest to get right. Convert to tabulation when you need speed or less stack.
**Space optimisation:** if `dp[i]` only uses `dp[i-1]`, keep only one or two rows.

```cpp
// Generic top-down template
vector<int> memo(n, -1);
function<int(int)> solve = [&](int i) -> int {
    if (i >= n) return 0;                     // base case
    int& res = memo[i];
    if (res != -1) return res;                // already computed
    res = max(solve(i + 1), a[i] + solve(i + 2));   // transition = choices
    return res;
};
```

---

## 11.1 1D Linear DP

**Signals**
- A sequence where the answer at position i depends on a **few previous positions**: climbing stairs, house robber, decode ways, word break.

**Intuition**
"To reach step i I came from i-1 or i-2" (count ways: add them).
"At house i, either rob it (take `a[i] + dp[i-2]`) or skip it (take `dp[i-1]`)" (optimise: take the max).
Circular arrays (House Robber II): run the linear version twice, once without the first element and once without the last.

**C++ template**
```cpp
int climbStairs(int n) {                       // dp[i] = dp[i-1] + dp[i-2]
    int a = 1, b = 1;
    for (int i = 2; i <= n; i++) { int c = a + b; a = b; b = c; }
    return b;
}

int rob(vector<int>& a) {                      // dp[i] = max(dp[i-1], dp[i-2] + a[i])
    int prev2 = 0, prev1 = 0;
    for (int x : a) { int cur = max(prev1, prev2 + x); prev2 = prev1; prev1 = cur; }
    return prev1;
}

int numDecodings(string s) {                   // one-digit and two-digit choices
    int n = s.size(); vector<int> dp(n + 1, 0); dp[0] = 1;
    for (int i = 1; i <= n; i++) {
        if (s[i - 1] != '0') dp[i] += dp[i - 1];
        if (i >= 2) { int two = stoi(s.substr(i - 2, 2)); if (two >= 10 && two <= 26) dp[i] += dp[i - 2]; }
    }
    return dp[n];
}

bool wordBreak(string s, vector<string>& dict) {   // dp[i] = s[0..i) can be segmented
    unordered_set<string> D(dict.begin(), dict.end());
    int n = s.size(); vector<bool> dp(n + 1, false); dp[0] = true;
    for (int i = 1; i <= n; i++)
        for (int j = 0; j < i && !dp[i]; j++)
            if (dp[j] && D.count(s.substr(j, i - j))) dp[i] = true;
    return dp[n];
}
```

**Complexity:** O(n), or O(n³) for Word Break (O(n²) split points, each building and looking up an O(n) substring).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [70. Climbing Stairs](https://leetcode.com/problems/climbing-stairs/) | Easy |
| 2 | [746. Min Cost Climbing Stairs](https://leetcode.com/problems/min-cost-climbing-stairs/) | Easy |
| 3 | [198. House Robber](https://leetcode.com/problems/house-robber/) | Medium |
| 4 | [213. House Robber II](https://leetcode.com/problems/house-robber-ii/) | Medium |
| 5 | [740. Delete and Earn](https://leetcode.com/problems/delete-and-earn/) | Medium |
| 6 | [91. Decode Ways](https://leetcode.com/problems/decode-ways/) | Medium |
| 7 | [139. Word Break](https://leetcode.com/problems/word-break/) | Medium |
| 8 | [2140. Solving Questions With Brainpower](https://leetcode.com/problems/solving-questions-with-brainpower/) | Medium |
| 9 | [983. Minimum Cost For Tickets](https://leetcode.com/problems/minimum-cost-for-tickets/) | Medium |
| 10 | [790. Domino and Tromino Tiling](https://leetcode.com/problems/domino-and-tromino-tiling/) | Medium |

---

## 11.2 0/1 Knapsack (Each Item Used At Most Once)

**Signals**
- "Choose a **subset** of items" with a **capacity / target sum**, where each item is used **once**.
- "Partition into two equal subsets", "assign + or - to reach a target", "minimise the difference of two piles".

**Intuition**
`dp[i][w]` = best value (or number of ways, or whether it's possible) using the first i items with capacity w.
For item i: **skip it** → `dp[i-1][w]`, or **take it** → `dp[i-1][w - wt[i]] + val[i]`.
**1D space trick:** loop w **downwards** so each item is counted at most once (`dp[w - wt]` still holds the previous row's value).

**Reductions to remember**
- **Partition Equal Subset Sum:** is there a subset with sum `total/2`?
- **Target Sum:** `P - N = target` and `P + N = total`, so `P = (total + target)/2`. Count the subsets with sum P.
- **Last Stone Weight II:** minimise `total - 2·S` where `S ≤ total/2`.

**C++ template**
```cpp
bool canPartition(vector<int>& a) {
    int sum = accumulate(a.begin(), a.end(), 0);
    if (sum & 1) return false;
    int T = sum / 2; vector<bool> dp(T + 1, false); dp[0] = true;
    for (int x : a)
        for (int w = T; w >= x; w--)               // DOWNWARD: each item used once
            dp[w] = dp[w] || dp[w - x];
    return dp[T];
}

int countSubsets(vector<int>& a, int T) {           // number of subsets with sum T
    vector<int> dp(T + 1, 0); dp[0] = 1;
    for (int x : a) for (int w = T; w >= x; w--) dp[w] += dp[w - x];
    return dp[T];
}

int knapsack(vector<int>& wt, vector<int>& val, int W) {
    vector<int> dp(W + 1, 0);
    for (int i = 0; i < (int)wt.size(); i++)
        for (int w = W; w >= wt[i]; w--)
            dp[w] = max(dp[w], dp[w - wt[i]] + val[i]);
    return dp[W];
}
```

**Complexity:** O(n · W) time, O(W) space.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [416. Partition Equal Subset Sum](https://leetcode.com/problems/partition-equal-subset-sum/) | Medium |
| 2 | [494. Target Sum](https://leetcode.com/problems/target-sum/) | Medium |
| 3 | [1049. Last Stone Weight II](https://leetcode.com/problems/last-stone-weight-ii/) | Medium |
| 4 | [474. Ones and Zeroes](https://leetcode.com/problems/ones-and-zeroes/) (2D capacity) | Medium |
| 5 | [1155. Number of Dice Rolls With Target Sum](https://leetcode.com/problems/number-of-dice-rolls-with-target-sum/) | Medium |
| 6 | [2915. Length of the Longest Subsequence That Sums to Target](https://leetcode.com/problems/length-of-the-longest-subsequence-that-sums-to-target/) | Medium |
| 7 | [2787. Ways to Express an Integer as Sum of Powers](https://leetcode.com/problems/ways-to-express-an-integer-as-sum-of-powers/) | Medium |
| 8 | [879. Profitable Schemes](https://leetcode.com/problems/profitable-schemes/) | Hard |
| 9 | [956. Tallest Billboard](https://leetcode.com/problems/tallest-billboard/) | Hard |
| 10 | [805. Split Array With Same Average](https://leetcode.com/problems/split-array-with-same-average/) | Hard |

---

## 11.3 Unbounded Knapsack (Items Reusable)

**Signals**
- "**Unlimited** supply of each coin / item", "minimum number of coins", "number of ways to make an amount", "perfect squares", "cut a rod".

**Intuition**
Same as 0/1 knapsack, but after taking an item you **stay on the same item**: `dp[i][w - wt]` instead of `dp[i-1][w - wt]`.
In 1D, loop w **upwards** so `dp[w - wt]` may already include this item.

**Order matters for counting:**
- **Combinations** (Coin Change II, `{1,2}` and `{2,1}` count once): loop **coins outside, amount inside**.
- **Permutations** (Combination Sum IV, order matters): loop **amount outside, coins inside**.

**C++ template**
```cpp
int coinChange(vector<int>& coins, int amount) {     // minimum number of coins
    vector<int> dp(amount + 1, INT_MAX); dp[0] = 0;
    for (int c : coins)
        for (int w = c; w <= amount; w++)             // UPWARD: reuse allowed
            if (dp[w - c] != INT_MAX) dp[w] = min(dp[w], dp[w - c] + 1);
    return dp[amount] == INT_MAX ? -1 : dp[amount];
}

int change(int amount, vector<int>& coins) {          // combinations
    vector<unsigned long long> dp(amount + 1, 0); dp[0] = 1;
    for (int c : coins) for (int w = c; w <= amount; w++) dp[w] += dp[w - c];
    return dp[amount];
}

int combinationSum4(vector<int>& nums, int target) {  // permutations (order matters)
    vector<unsigned long long> dp(target + 1, 0); dp[0] = 1;
    for (int w = 1; w <= target; w++)
        for (int x : nums) if (w >= x) dp[w] += dp[w - x];
    return dp[target];
}
```

**Complexity:** O(n · W).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [322. Coin Change](https://leetcode.com/problems/coin-change/) | Medium |
| 2 | [518. Coin Change II](https://leetcode.com/problems/coin-change-ii/) | Medium |
| 3 | [377. Combination Sum IV](https://leetcode.com/problems/combination-sum-iv/) | Medium |
| 4 | [279. Perfect Squares](https://leetcode.com/problems/perfect-squares/) | Medium |
| 5 | [343. Integer Break](https://leetcode.com/problems/integer-break/) | Medium |
| 6 | [2466. Count Ways To Build Good Strings](https://leetcode.com/problems/count-ways-to-build-good-strings/) | Medium |
| 7 | [650. 2 Keys Keyboard](https://leetcode.com/problems/2-keys-keyboard/) | Medium |
| 8 | [1774. Closest Dessert Cost](https://leetcode.com/problems/closest-dessert-cost/) | Medium |
| 9 | [1449. Form Largest Integer With Digits That Add up to Target](https://leetcode.com/problems/form-largest-integer-with-digits-that-add-up-to-target/) | Hard |
| 10 | [2585. Number of Ways to Earn Points](https://leetcode.com/problems/number-of-ways-to-earn-points/) (bounded knapsack) | Hard |

---

## 11.4 Grid DP

**Signals**
- Moving on a grid **only right / down** (or downward with a few allowed moves), counting paths, minimum path sum, largest square.

**Intuition**
`dp[r][c]` depends on `dp[r-1][c]` and `dp[r][c-1]`, the only ways to arrive. Fill row by row.
**Maximal square:** `dp[r][c] = 1 + min(top, left, top-left)`, because a square is only as large as its smallest neighbouring square allows.
**Dungeon Game:** fill from the **bottom-right** backwards, since what you need depends on what lies ahead.
**Two walkers** (Cherry Pickup): move both at once; the state is `(step, c1, c2)`.

**C++ template**
```cpp
int uniquePathsWithObstacles(vector<vector<int>>& g) {
    int R = g.size(), C = g[0].size(); vector<long long> dp(C, 0); dp[0] = 1;
    for (int r = 0; r < R; r++)
        for (int c = 0; c < C; c++) {
            if (g[r][c]) dp[c] = 0;
            else if (c > 0) dp[c] += dp[c - 1];          // dp[c] (from above) + dp[c-1] (from the left)
        }
    return dp[C - 1];
}

int minPathSum(vector<vector<int>>& g) {
    int R = g.size(), C = g[0].size();
    for (int r = 0; r < R; r++)
        for (int c = 0; c < C; c++) {
            if (r == 0 && c == 0) continue;
            int up = r ? g[r - 1][c] : INT_MAX, left = c ? g[r][c - 1] : INT_MAX;
            g[r][c] += min(up, left);
        }
    return g[R - 1][C - 1];
}

int maximalSquare(vector<vector<char>>& m) {
    int R = m.size(), C = m[0].size(), best = 0;
    vector<vector<int>> dp(R + 1, vector<int>(C + 1, 0));
    for (int r = 1; r <= R; r++)
        for (int c = 1; c <= C; c++)
            if (m[r - 1][c - 1] == '1') {
                dp[r][c] = 1 + min({dp[r - 1][c], dp[r][c - 1], dp[r - 1][c - 1]});
                best = max(best, dp[r][c]);
            }
    return best * best;
}
```

**Complexity:** O(R·C) time, O(C) space when optimised.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [62. Unique Paths](https://leetcode.com/problems/unique-paths/) | Medium |
| 2 | [63. Unique Paths II](https://leetcode.com/problems/unique-paths-ii/) | Medium |
| 3 | [64. Minimum Path Sum](https://leetcode.com/problems/minimum-path-sum/) | Medium |
| 4 | [120. Triangle](https://leetcode.com/problems/triangle/) | Medium |
| 5 | [931. Minimum Falling Path Sum](https://leetcode.com/problems/minimum-falling-path-sum/) | Medium |
| 6 | [221. Maximal Square](https://leetcode.com/problems/maximal-square/) | Medium |
| 7 | [1277. Count Square Submatrices with All Ones](https://leetcode.com/problems/count-square-submatrices-with-all-ones/) | Medium |
| 8 | [174. Dungeon Game](https://leetcode.com/problems/dungeon-game/) | Hard |
| 9 | [1463. Cherry Pickup II](https://leetcode.com/problems/cherry-pickup-ii/) | Hard |
| 10 | [741. Cherry Pickup](https://leetcode.com/problems/cherry-pickup/) | Hard |

---

## 11.5 Two Strings / Two Sequences (LCS Family)

**Signals**
- Two strings or arrays: "longest common subsequence", "edit distance", "minimum deletions to make them equal", "is s3 an interleaving of s1 and s2", "wildcard / regex matching", "distinct subsequences".

**Intuition**
`dp[i][j]` = answer for the prefixes `s[0..i)` and `t[0..j)`. Compare the **last characters**:
- **Match** (`s[i-1] == t[j-1]`): usually `dp[i-1][j-1] + something`.
- **No match**: try dropping the last char of s (`dp[i-1][j]`) or of t (`dp[i][j-1]`), or replacing (`dp[i-1][j-1]`), and take the best.

Use an extra row and column for empty prefixes, which keeps the base cases simple.

| Problem | Match | Mismatch |
|---------|-------|----------|
| LCS | `dp[i-1][j-1] + 1` | `max(dp[i-1][j], dp[i][j-1])` |
| Edit Distance | `dp[i-1][j-1]` | `1 + min(insert dp[i][j-1], delete dp[i-1][j], replace dp[i-1][j-1])` |
| Distinct Subsequences (count t in s) | `dp[i-1][j-1] + dp[i-1][j]` | `dp[i-1][j]` |
| Longest common **substring** | `dp[i-1][j-1] + 1` | `0` |

**C++ template**
```cpp
int longestCommonSubsequence(string a, string b) {
    int n = a.size(), m = b.size();
    vector<vector<int>> dp(n + 1, vector<int>(m + 1, 0));
    for (int i = 1; i <= n; i++)
        for (int j = 1; j <= m; j++)
            dp[i][j] = a[i - 1] == b[j - 1] ? dp[i - 1][j - 1] + 1
                                            : max(dp[i - 1][j], dp[i][j - 1]);
    return dp[n][m];
}

int minDistance(string a, string b) {           // Edit Distance
    int n = a.size(), m = b.size();
    vector<vector<int>> dp(n + 1, vector<int>(m + 1));
    for (int i = 0; i <= n; i++) dp[i][0] = i;
    for (int j = 0; j <= m; j++) dp[0][j] = j;
    for (int i = 1; i <= n; i++)
        for (int j = 1; j <= m; j++)
            dp[i][j] = a[i - 1] == b[j - 1] ? dp[i - 1][j - 1]
                     : 1 + min({dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1]});
    return dp[n][m];
}

bool isMatchWildcard(string s, string p) {       // '?' = one char, '*' = any sequence
    int n = s.size(), m = p.size();
    vector<vector<bool>> dp(n + 1, vector<bool>(m + 1, false)); dp[0][0] = true;
    for (int j = 1; j <= m; j++) if (p[j - 1] == '*') dp[0][j] = dp[0][j - 1];
    for (int i = 1; i <= n; i++)
        for (int j = 1; j <= m; j++) {
            if (p[j - 1] == '*') dp[i][j] = dp[i][j - 1] || dp[i - 1][j];   // '*' matches empty, or one more char
            else dp[i][j] = dp[i - 1][j - 1] && (p[j - 1] == '?' || p[j - 1] == s[i - 1]);
        }
    return dp[n][m];
}
```

**Complexity:** O(n·m) time, O(m) space when optimised.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [1143. Longest Common Subsequence](https://leetcode.com/problems/longest-common-subsequence/) | Medium |
| 2 | [583. Delete Operation for Two Strings](https://leetcode.com/problems/delete-operation-for-two-strings/) | Medium |
| 3 | [712. Minimum ASCII Delete Sum for Two Strings](https://leetcode.com/problems/minimum-ascii-delete-sum-for-two-strings/) | Medium |
| 4 | [718. Maximum Length of Repeated Subarray](https://leetcode.com/problems/maximum-length-of-repeated-subarray/) | Medium |
| 5 | [72. Edit Distance](https://leetcode.com/problems/edit-distance/) | Medium |
| 6 | [97. Interleaving String](https://leetcode.com/problems/interleaving-string/) | Medium |
| 7 | [115. Distinct Subsequences](https://leetcode.com/problems/distinct-subsequences/) | Hard |
| 8 | [1092. Shortest Common Supersequence](https://leetcode.com/problems/shortest-common-supersequence/) | Hard |
| 9 | [44. Wildcard Matching](https://leetcode.com/problems/wildcard-matching/) | Hard |
| 10 | [10. Regular Expression Matching](https://leetcode.com/problems/regular-expression-matching/) | Hard |

---

## 11.6 Longest Increasing Subsequence (LIS) Family

**Signals**
- "Longest increasing / divisible / chain / arithmetic subsequence", "Russian doll envelopes", "minimum removals to make a mountain".

**Intuition**
**O(n²):** `dp[i]` = length of the longest valid subsequence **ending at i** = `1 + max(dp[j])` over `j < i` where `j → i` is allowed.
**O(n log n) patience sorting:** `tails[k]` = the smallest possible tail of an increasing subsequence of length k+1.
For each x, `lower_bound` finds where x goes: replace that tail, or append if x is bigger than every tail.
`tails` is not the actual subsequence, but its length is the LIS length.

**2D (envelopes):** sort by width ascending and **height descending** for equal widths, then run LIS on the heights. The descending tiebreak stops two envelopes of the same width from nesting.

**C++ template**
```cpp
int lengthOfLIS(vector<int>& a) {                       // O(n log n)
    vector<int> tails;
    for (int x : a) {
        auto it = lower_bound(tails.begin(), tails.end(), x);   // upper_bound for non-decreasing
        if (it == tails.end()) tails.push_back(x); else *it = x;
    }
    return tails.size();
}

// O(n^2) with a count of LIS (Number of LIS)
int findNumberOfLIS(vector<int>& a) {
    int n = a.size(); vector<int> len(n, 1), cnt(n, 1);
    for (int i = 0; i < n; i++)
        for (int j = 0; j < i; j++) if (a[j] < a[i]) {
            if (len[j] + 1 > len[i]) { len[i] = len[j] + 1; cnt[i] = cnt[j]; }
            else if (len[j] + 1 == len[i]) cnt[i] += cnt[j];
        }
    int L = *max_element(len.begin(), len.end()), res = 0;
    for (int i = 0; i < n; i++) if (len[i] == L) res += cnt[i];
    return res;
}

int maxEnvelopes(vector<vector<int>>& e) {
    sort(e.begin(), e.end(), [](auto& x, auto& y) {
        return x[0] == y[0] ? x[1] > y[1] : x[0] < y[0];
    });
    vector<int> h; for (auto& v : e) h.push_back(v[1]);
    return lengthOfLIS(h);
}
```

**Complexity:** O(n log n) or O(n²).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [300. Longest Increasing Subsequence](https://leetcode.com/problems/longest-increasing-subsequence/) | Medium |
| 2 | [646. Maximum Length of Pair Chain](https://leetcode.com/problems/maximum-length-of-pair-chain/) | Medium |
| 3 | [1218. Longest Arithmetic Subsequence of Given Difference](https://leetcode.com/problems/longest-arithmetic-subsequence-of-given-difference/) | Medium |
| 4 | [368. Largest Divisible Subset](https://leetcode.com/problems/largest-divisible-subset/) | Medium |
| 5 | [1048. Longest String Chain](https://leetcode.com/problems/longest-string-chain/) | Medium |
| 6 | [673. Number of Longest Increasing Subsequence](https://leetcode.com/problems/number-of-longest-increasing-subsequence/) | Medium |
| 7 | [1027. Longest Arithmetic Subsequence](https://leetcode.com/problems/longest-arithmetic-subsequence/) | Medium |
| 8 | [354. Russian Doll Envelopes](https://leetcode.com/problems/russian-doll-envelopes/) | Hard |
| 9 | [1671. Minimum Number of Removals to Make Mountain Array](https://leetcode.com/problems/minimum-number-of-removals-to-make-mountain-array/) | Hard |
| 10 | [1964. Find the Longest Valid Obstacle Course at Each Position](https://leetcode.com/problems/find-the-longest-valid-obstacle-course-at-each-position/) | Hard |

---

## 11.7 Palindrome DP (Single String, Ranges)

**Signals**
- "Longest palindromic substring / subsequence", "count palindromic substrings", "minimum cuts or insertions to make palindromes".

**Intuition**
`isPal[i][j] = s[i] == s[j] && isPal[i+1][j-1]`. Fill by **increasing length**, or by i descending, so the inner range is ready first.
**Expand around the centre** (2n-1 centres) avoids the table for substring questions: O(n²) time, O(1) space.
**Longest palindromic subsequence** = `LCS(s, reverse(s))`, or directly `dp[i][j] = s[i]==s[j] ? 2 + dp[i+1][j-1] : max(dp[i+1][j], dp[i][j-1])`.
**Minimum insertions** to make a palindrome = `n - LPS`.
**Minimum cuts:** `cut[j] = min(cut[i-1] + 1)` over all i where `s[i..j]` is a palindrome.

**C++ template**
```cpp
string longestPalindrome(string s) {             // expand around centre
    int start = 0, best = 0, n = s.size();
    auto expand = [&](int l, int r) {
        while (l >= 0 && r < n && s[l] == s[r]) { l--; r++; }
        if (r - l - 1 > best) { best = r - l - 1; start = l + 1; }
    };
    for (int i = 0; i < n; i++) { expand(i, i); expand(i, i + 1); }
    return s.substr(start, best);
}

int longestPalindromeSubseq(string s) {
    int n = s.size(); vector<vector<int>> dp(n, vector<int>(n, 0));
    for (int i = n - 1; i >= 0; i--) {
        dp[i][i] = 1;
        for (int j = i + 1; j < n; j++)
            dp[i][j] = s[i] == s[j] ? 2 + dp[i + 1][j - 1] : max(dp[i + 1][j], dp[i][j - 1]);
    }
    return dp[0][n - 1];
}

int minCut(string s) {                           // Palindrome Partitioning II
    int n = s.size(); vector<vector<bool>> pal(n, vector<bool>(n, false)); vector<int> cut(n);
    for (int j = 0; j < n; j++) {
        cut[j] = j;                              // worst case: every character is its own piece
        for (int i = 0; i <= j; i++)
            if (s[i] == s[j] && (j - i < 2 || pal[i + 1][j - 1])) {
                pal[i][j] = true;
                cut[j] = i == 0 ? 0 : min(cut[j], cut[i - 1] + 1);
            }
    }
    return cut[n - 1];
}
```

**Complexity:** O(n²).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [5. Longest Palindromic Substring](https://leetcode.com/problems/longest-palindromic-substring/) | Medium |
| 2 | [647. Palindromic Substrings](https://leetcode.com/problems/palindromic-substrings/) | Medium |
| 3 | [516. Longest Palindromic Subsequence](https://leetcode.com/problems/longest-palindromic-subsequence/) | Medium |
| 4 | [1312. Minimum Insertion Steps to Make a String Palindrome](https://leetcode.com/problems/minimum-insertion-steps-to-make-a-string-palindrome/) | Hard |
| 5 | [132. Palindrome Partitioning II](https://leetcode.com/problems/palindrome-partitioning-ii/) | Hard |
| 6 | [1745. Palindrome Partitioning IV](https://leetcode.com/problems/palindrome-partitioning-iv/) | Hard |
| 7 | [1278. Palindrome Partitioning III](https://leetcode.com/problems/palindrome-partitioning-iii/) | Hard |
| 8 | [2472. Maximum Number of Non-overlapping Palindrome Substrings](https://leetcode.com/problems/maximum-number-of-non-overlapping-palindrome-substrings/) | Hard |
| 9 | [2484. Count Palindromic Subsequences](https://leetcode.com/problems/count-palindromic-subsequences/) | Hard |
| 10 | [730. Count Different Palindromic Subsequences](https://leetcode.com/problems/count-different-palindromic-subsequences/) | Hard |

---

## 11.8 Interval DP / Matrix Chain Multiplication (MCM)

**Signals**
- The answer for range `[i..j]` depends on **choosing a split point k** inside it: "burst balloons", "merge stones", "minimum cost to cut a stick", "triangulate a polygon".
- Two-player games on the ends of an array ("stone game", "predict the winner").
- n is usually ≤ 500 (O(n³)).

**Intuition**
`dp[i][j] = best over k in [i..j) of dp[i][k] + dp[k+1][j] + cost(i, k, j)`.
Fill by **increasing length** so smaller intervals are ready.
**Burst Balloons trick:** think of k as the **last** balloon burst in `(i, j)`, not the first. Then its neighbours at that moment are exactly i and j, so the subproblems become independent.
**Games:** `dp[i][j]` = (my score - opponent's score) on `a[i..j]` = `max(a[i] - dp[i+1][j], a[j] - dp[i][j-1])`.

**C++ template**
```cpp
int maxCoins(vector<int>& nums) {                       // Burst Balloons
    vector<int> a = {1}; a.insert(a.end(), nums.begin(), nums.end()); a.push_back(1);
    int n = a.size(); vector<vector<int>> dp(n, vector<int>(n, 0));
    for (int len = 2; len < n; len++)                   // open interval (i, j)
        for (int i = 0; i + len < n; i++) {
            int j = i + len;
            for (int k = i + 1; k < j; k++)             // k = last balloon burst
                dp[i][j] = max(dp[i][j], dp[i][k] + dp[k][j] + a[i] * a[k] * a[j]);
        }
    return dp[0][n - 1];
}

// Minimum Cost to Cut a Stick: add 0 and n as cut points, then the same structure with cost = c[j] - c[i]

bool predictTheWinner(vector<int>& a) {
    int n = a.size(); vector<vector<int>> dp(n, vector<int>(n, 0));
    for (int i = n - 1; i >= 0; i--) {
        dp[i][i] = a[i];
        for (int j = i + 1; j < n; j++)
            dp[i][j] = max(a[i] - dp[i + 1][j], a[j] - dp[i][j - 1]);
    }
    return dp[0][n - 1] >= 0;
}
```

**Complexity:** O(n³) time, O(n²) space.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [877. Stone Game](https://leetcode.com/problems/stone-game/) | Medium |
| 2 | [486. Predict the Winner](https://leetcode.com/problems/predict-the-winner/) | Medium |
| 3 | [375. Guess Number Higher or Lower II](https://leetcode.com/problems/guess-number-higher-or-lower-ii/) | Medium |
| 4 | [1039. Minimum Score Triangulation of Polygon](https://leetcode.com/problems/minimum-score-triangulation-of-polygon/) | Medium |
| 5 | [1130. Minimum Cost Tree From Leaf Values](https://leetcode.com/problems/minimum-cost-tree-from-leaf-values/) | Medium |
| 6 | [1547. Minimum Cost to Cut a Stick](https://leetcode.com/problems/minimum-cost-to-cut-a-stick/) | Hard |
| 7 | [312. Burst Balloons](https://leetcode.com/problems/burst-balloons/) | Hard |
| 8 | [664. Strange Printer](https://leetcode.com/problems/strange-printer/) | Hard |
| 9 | [1000. Minimum Cost to Merge Stones](https://leetcode.com/problems/minimum-cost-to-merge-stones/) | Hard |
| 10 | [546. Remove Boxes](https://leetcode.com/problems/remove-boxes/) | Hard |

---

## 11.9 State Machine DP (Stocks & Friends)

**Signals**
- At each step you are in one of a few **modes**: holding / not holding a stock, cooldown, transactions used, last move up / down, number of consecutive absences.

**Intuition**
Add the mode to the state: `dp[i][state]`. Draw the state diagram: which actions move you between states,
and what each one costs or earns. Every arrow in the diagram becomes one term in the transition.

```
                buy (-price)
   NOT HOLD  ───────────────►  HOLD
      ▲  ◄───────────────────   │
      │       sell (+price)     │
   (rest)                    (rest)
```

**C++ template**
```cpp
int maxProfitII(vector<int>& p) {                      // unlimited transactions
    int hold = INT_MIN, cash = 0;
    for (int x : p) {
        int prevCash = cash;
        cash = max(cash, hold + x);                    // sell
        hold = max(hold, prevCash - x);                // buy
    }
    return cash;
}

int maxProfitCooldown(vector<int>& p) {
    int hold = INT_MIN, sold = 0, rest = 0;
    for (int x : p) {
        int prevSold = sold;
        sold = hold + x;                               // sell today
        hold = max(hold, rest - x);                    // can only buy from rest
        rest = max(rest, prevSold);                    // cooldown
    }
    return max(sold, rest);
}

int maxProfitK(int k, vector<int>& p) {                // at most k transactions
    vector<int> buy(k + 1, INT_MIN), sell(k + 1, 0);
    for (int x : p)
        for (int t = 1; t <= k; t++) {
            buy[t]  = max(buy[t], sell[t - 1] - x);
            sell[t] = max(sell[t], buy[t] + x);
        }
    return sell[k];
}
```

**Complexity:** O(n · states).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [121. Best Time to Buy and Sell Stock](https://leetcode.com/problems/best-time-to-buy-and-sell-stock/) | Easy |
| 2 | [122. Best Time to Buy and Sell Stock II](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-ii/) | Medium |
| 3 | [309. Best Time to Buy and Sell Stock with Cooldown](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-with-cooldown/) | Medium |
| 4 | [714. Best Time to Buy and Sell Stock with Transaction Fee](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-with-transaction-fee/) | Medium |
| 5 | [376. Wiggle Subsequence](https://leetcode.com/problems/wiggle-subsequence/) | Medium |
| 6 | [1911. Maximum Alternating Subsequence Sum](https://leetcode.com/problems/maximum-alternating-subsequence-sum/) | Medium |
| 7 | [123. Best Time to Buy and Sell Stock III](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-iii/) | Hard |
| 8 | [188. Best Time to Buy and Sell Stock IV](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-iv/) | Hard |
| 9 | [1220. Count Vowels Permutation](https://leetcode.com/problems/count-vowels-permutation/) | Hard |
| 10 | [552. Student Attendance Record II](https://leetcode.com/problems/student-attendance-record-ii/) | Hard |

---

## 11.10 DP on Trees

**Signals**
- An optimisation or count on a tree where each node's choice affects its neighbours: "rob houses in a tree", "place cameras", "sum of distances to all nodes".

**Intuition**
Post-order DFS that returns a **small tuple of states** per node, e.g. `{robbed, notRobbed}` or `{covered, hasCamera, needsCover}`.
The parent combines its children's tuples.
**Rerooting** (answer for *every* node as the root): run one DFS to get subtree answers, then a second DFS that
passes the "outside" contribution down: `ans[child] = ans[parent] - size[child] + (n - size[child])`.

**C++ template**
```cpp
pair<int,int> robTree(TreeNode* r) {             // {rob this node, skip this node}
    if (!r) return {0, 0};
    auto L = robTree(r->left), R = robTree(r->right);
    int take = r->val + L.second + R.second;
    int skip = max(L.first, L.second) + max(R.first, R.second);
    return {take, skip};
}

// Sum of Distances in Tree (rerooting)
vector<vector<int>> adj; vector<int> sz, ans; int N;
void dfs1(int u, int p, int depth) {
    sz[u] = 1; ans[0] += depth;
    for (int v : adj[u]) if (v != p) { dfs1(v, u, depth + 1); sz[u] += sz[v]; }
}
void dfs2(int u, int p) {
    for (int v : adj[u]) if (v != p) {
        ans[v] = ans[u] - sz[v] + (N - sz[v]);   // moving the root u -> v
        dfs2(v, u);
    }
}

int numTrees(int n) {                            // Catalan numbers: unique BSTs
    vector<long long> dp(n + 1, 0); dp[0] = dp[1] = 1;
    for (int i = 2; i <= n; i++)
        for (int root = 1; root <= i; root++) dp[i] += dp[root - 1] * dp[i - root];
    return dp[n];
}
```

**Complexity:** O(n) for the tree DP and rerooting; O(n²) for the Catalan count `numTrees`.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [96. Unique Binary Search Trees](https://leetcode.com/problems/unique-binary-search-trees/) | Medium |
| 2 | [95. Unique Binary Search Trees II](https://leetcode.com/problems/unique-binary-search-trees-ii/) | Medium |
| 3 | [337. House Robber III](https://leetcode.com/problems/house-robber-iii/) | Medium |
| 4 | [979. Distribute Coins in Binary Tree](https://leetcode.com/problems/distribute-coins-in-binary-tree/) | Medium |
| 5 | [1339. Maximum Product of Splitted Binary Tree](https://leetcode.com/problems/maximum-product-of-splitted-binary-tree/) | Medium |
| 6 | [1145. Binary Tree Coloring Game](https://leetcode.com/problems/binary-tree-coloring-game/) | Medium |
| 7 | [968. Binary Tree Cameras](https://leetcode.com/problems/binary-tree-cameras/) | Hard |
| 8 | [2246. Longest Path With Different Adjacent Characters](https://leetcode.com/problems/longest-path-with-different-adjacent-characters/) | Hard |
| 9 | [834. Sum of Distances in Tree](https://leetcode.com/problems/sum-of-distances-in-tree/) | Hard |
| 10 | [2858. Minimum Edge Reversals So Every Node Is Reachable](https://leetcode.com/problems/minimum-edge-reversals-so-every-node-is-reachable/) | Hard |

---

## 11.11 Bitmask DP

**Signals**
- **n ≤ 20** (often ≤ 16) and you need to track **which items are used / visited**: assign tasks to workers, visit all nodes (TSP), split into groups.

**Intuition**
Store the set of used items as a bitmask: `dp[mask]` (or `dp[mask][last]`) = best result when exactly the items in `mask` are used.
There are 2ⁿ states, and each transition adds one unused item: `dp[mask | 1<<j]` from `dp[mask]`.
- **Assignment** (n workers, n jobs): the next person is `popcount(mask)`, so you don't need an extra dimension.
- **TSP / shortest superstring:** `dp[mask][last]`, O(2ⁿ · n²).
- **Shortest path visiting all nodes:** BFS over the state `(node, mask)`.

**C++ template**
```cpp
// Minimum XOR Sum of Two Arrays: assign each a[i] to a distinct b[j]
int minimumXORSum(vector<int>& a, vector<int>& b) {
    int n = a.size(); vector<int> dp(1 << n, INT_MAX); dp[0] = 0;
    for (int mask = 0; mask < (1 << n); mask++) {
        if (dp[mask] == INT_MAX) continue;
        int i = __builtin_popcount(mask);               // the next a[i] to assign
        if (i == n) continue;
        for (int j = 0; j < n; j++) if (!(mask >> j & 1))
            dp[mask | 1 << j] = min(dp[mask | 1 << j], dp[mask] + (a[i] ^ b[j]));
    }
    return dp[(1 << n) - 1];
}

// TSP-style: dp[mask][last]
// dp[1 << s][s] = 0
// dp[mask | 1 << nxt][nxt] = min(..., dp[mask][last] + cost[last][nxt])  for nxt not in mask
```

**Complexity:** O(2ⁿ · n) or O(2ⁿ · n²).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [464. Can I Win](https://leetcode.com/problems/can-i-win/) | Medium |
| 2 | [1986. Minimum Number of Work Sessions to Finish the Tasks](https://leetcode.com/problems/minimum-number-of-work-sessions-to-finish-the-tasks/) | Medium |
| 3 | [2305. Fair Distribution of Cookies](https://leetcode.com/problems/fair-distribution-of-cookies/) | Medium |
| 4 | [847. Shortest Path Visiting All Nodes](https://leetcode.com/problems/shortest-path-visiting-all-nodes/) | Hard |
| 5 | [1879. Minimum XOR Sum of Two Arrays](https://leetcode.com/problems/minimum-xor-sum-of-two-arrays/) | Hard |
| 6 | [1125. Smallest Sufficient Team](https://leetcode.com/problems/smallest-sufficient-team/) | Hard |
| 7 | [1434. Number of Ways to Wear Different Hats to Each Other](https://leetcode.com/problems/number-of-ways-to-wear-different-hats-to-each-other/) | Hard |
| 8 | [943. Find the Shortest Superstring](https://leetcode.com/problems/find-the-shortest-superstring/) | Hard |
| 9 | [1595. Minimum Cost to Connect Two Groups of Points](https://leetcode.com/problems/minimum-cost-to-connect-two-groups-of-points/) | Hard |
| 10 | [1349. Maximum Students Taking Exam](https://leetcode.com/problems/maximum-students-taking-exam/) | Hard |

---

## 11.12 Digit DP

**Signals**
- "Count integers in `[L, R]` (R up to 10¹⁸) whose **digits** satisfy a property": unique digits, no consecutive ones, digit sum, stepping numbers.

**Intuition**
Build the number **digit by digit from the most significant digit**. Track:
- `pos`: the current digit index
- `tight`: whether the prefix so far equals N's prefix (if so, the next digit can only go up to `N[pos]`; otherwise up to 9)
- `started`: whether a non-zero digit has been placed yet (false while we're still in leading zeros)
- plus whatever the property needs (a mask of used digits, the previous digit, a sum mod k...)

Answer for `[L, R]` = `count(R) - count(L-1)`. Memoise only the states where `tight == false` (or include tight in the key).

**C++ template**
```cpp
// Count numbers in [1, N] with all distinct digits
string S; int memo[20][1 << 10];                  // pos < 20: a long long has at most 19 digits
int go(int pos, int mask, bool tight, bool started) {
    if (pos == (int)S.size()) return started ? 1 : 0;
    if (!tight && started && memo[pos][mask] != -1) return memo[pos][mask];
    int limit = tight ? S[pos] - '0' : 9, res = 0;
    for (int d = 0; d <= limit; d++) {
        if (started && (mask >> d & 1)) continue;          // digit already used
        bool nowStarted = started || d != 0;
        int nmask = nowStarted ? mask | (1 << d) : 0;
        res += go(pos + 1, nmask, tight && d == limit, nowStarted);
    }
    if (!tight && started) memo[pos][mask] = res;
    return res;
}
int countDistinct(long long N) {
    S = to_string(N); memset(memo, -1, sizeof memo);
    return go(0, 0, true, false);
}
```

**Complexity:** O(digits · states · 10).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [357. Count Numbers with Unique Digits](https://leetcode.com/problems/count-numbers-with-unique-digits/) | Medium |
| 2 | [788. Rotated Digits](https://leetcode.com/problems/rotated-digits/) | Medium |
| 3 | [233. Number of Digit One](https://leetcode.com/problems/number-of-digit-one/) | Hard |
| 4 | [902. Numbers At Most N Given Digit Set](https://leetcode.com/problems/numbers-at-most-n-given-digit-set/) | Hard |
| 5 | [600. Non-negative Integers without Consecutive Ones](https://leetcode.com/problems/non-negative-integers-without-consecutive-ones/) | Hard |
| 6 | [1012. Numbers With Repeated Digits](https://leetcode.com/problems/numbers-with-repeated-digits/) | Hard |
| 7 | [2376. Count Special Integers](https://leetcode.com/problems/count-special-integers/) | Hard |
| 8 | [2719. Count of Integers](https://leetcode.com/problems/count-of-integers/) | Hard |
| 9 | [2801. Count Stepping Numbers in Range](https://leetcode.com/problems/count-stepping-numbers-in-range/) | Hard |
| 10 | [2827. Number of Beautiful Integers in the Range](https://leetcode.com/problems/number-of-beautiful-integers-in-the-range/) | Hard |

---

## DP Pattern Quick Reference

| Pattern | State | Typical transition |
|---------|-------|--------------------|
| 1D linear | `dp[i]` | `f(dp[i-1], dp[i-2], ...)` |
| 0/1 knapsack | `dp[w]` (w descending) | `max(dp[w], dp[w-wt] + val)` |
| Unbounded knapsack | `dp[w]` (w ascending) | `min(dp[w], dp[w-c] + 1)` |
| Grid | `dp[r][c]` | `f(dp[r-1][c], dp[r][c-1])` |
| Two strings | `dp[i][j]` | match → `dp[i-1][j-1]`, else best of the neighbours |
| LIS | `dp[i]` ending at i / `tails` | `1 + max(dp[j])` / `lower_bound` |
| Palindrome / range | `dp[i][j]` | from `dp[i+1][j-1]`, `dp[i+1][j]`, `dp[i][j-1]` |
| Interval (MCM) | `dp[i][j]` | `min/max over k of dp[i][k] + dp[k][j] + cost` |
| State machine | `dp[i][state]` | one term per arrow in the state diagram |
| Tree | tuple per node | combine the children's tuples (post-order) |
| Bitmask | `dp[mask]` / `dp[mask][last]` | add one unused item |
| Digit | `(pos, tight, started, extra)` | try each digit up to the limit |
