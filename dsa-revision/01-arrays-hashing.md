# 1. Arrays & Hashing

Arrays are the base for most other patterns. The core question in most array problems is:
**"Can I avoid recomputing something by remembering it?"** You can remember it in a hash map,
a prefix sum, a running best (Kadane), or the array's own indices (cyclic sort).

---

## 1.1 Hashing / Frequency Counting

**Signals**
- "Find if there exists...", "count occurrences", "duplicates", "anagram", "group by".
- A brute force with a nested loop that searches for a *complement* or *match*.

**Intuition**
A nested loop spends O(n) answering "have I seen X before?". A hash set or map answers the same
question in O(1) on average. So you trade **space for time**: remember what you've seen while you scan.

**Approach**
1. Decide what to store: the *value* (set), *value → count* (frequency), *value → index*, or *key → group*.
2. Scan once. For each element, **first query** the map (has my complement or match been seen?), **then insert** the current element.
3. For grouping, build a *canonical key*, e.g. a sorted string or a count signature for anagrams.

**C++ template**
```cpp
// Complement lookup (Two Sum style)
vector<int> twoSum(vector<int>& a, int target) {
    unordered_map<int,int> idx;            // value -> index
    for (int i = 0; i < (int)a.size(); i++) {
        auto it = idx.find(target - a[i]);
        if (it != idx.end()) return {it->second, i};
        idx[a[i]] = i;
    }
    return {};
}

// Frequency count
unordered_map<int,int> freq;
for (int x : a) freq[x]++;

// Group by canonical key (Group Anagrams)
unordered_map<string, vector<string>> groups;
for (auto& s : strs) {
    string key = s; sort(key.begin(), key.end());
    groups[key].push_back(s);
}
```

**Complexity:** O(n) time and O(n) space on average.
**Tip:** when keys are only lowercase letters, `int cnt[26]` is faster than `unordered_map`.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [1. Two Sum](https://leetcode.com/problems/two-sum/) | Easy |
| 2 | [217. Contains Duplicate](https://leetcode.com/problems/contains-duplicate/) | Easy |
| 3 | [242. Valid Anagram](https://leetcode.com/problems/valid-anagram/) | Easy |
| 4 | [205. Isomorphic Strings](https://leetcode.com/problems/isomorphic-strings/) | Easy |
| 5 | [290. Word Pattern](https://leetcode.com/problems/word-pattern/) | Easy |
| 6 | [49. Group Anagrams](https://leetcode.com/problems/group-anagrams/) | Medium |
| 7 | [347. Top K Frequent Elements](https://leetcode.com/problems/top-k-frequent-elements/) | Medium |
| 8 | [36. Valid Sudoku](https://leetcode.com/problems/valid-sudoku/) | Medium |
| 9 | [128. Longest Consecutive Sequence](https://leetcode.com/problems/longest-consecutive-sequence/) | Medium |
| 10 | [380. Insert Delete GetRandom O(1)](https://leetcode.com/problems/insert-delete-getrandom-o1/) | Medium |

---

## 1.2 Prefix Sum & Difference Array

**Signals**
- "Sum of subarray / range `[l, r]`", many range queries.
- "Number of subarrays with sum = K" (or divisible by K, or equal 0s and 1s), **negatives allowed**.
  Negatives rule out a sliding window.
- "Add value v to every element in `[l, r]`" many times: use a difference array.

**Intuition**
`pre[i]` = sum of the first i elements, so `sum(l..r) = pre[r+1] - pre[l]`.
A subarray `(j, i]` has sum K **iff** `pre[i] - pre[j] = K`, which means `pre[j] = pre[i] - K`.
That turns "count subarrays" into a hash-map lookup on earlier prefix values, the same idea as Two Sum.
A **difference array** does the reverse: mark `+v` at `l` and `-v` at `r+1`, and a prefix sum over it spreads each update across its range.

**Approach**
1. Range-sum queries: build `pre` of size n+1 once, then answer each query in O(1).
2. Counting subarrays: keep `map<prefix, count>` initialised with `{0: 1}`. For each running sum `s`, add `cnt[s-K]` to the answer, then increment `cnt[s]`.
3. For "divisible by K" use `((s % K) + K) % K` as the key. For "equal 0s and 1s" turn each 0 into -1 and look for sum 0.
4. 2D: `P[i+1][j+1] = a[i][j] + P[i][j+1] + P[i+1][j] - P[i][j]`.

**C++ template**
```cpp
// 1D prefix
vector<ll> pre(n + 1, 0);
for (int i = 0; i < n; i++) pre[i + 1] = pre[i] + a[i];
auto rangeSum = [&](int l, int r) { return pre[r + 1] - pre[l]; };

// Count subarrays with sum == k
int subarraySum(vector<int>& a, int k) {
    unordered_map<ll,int> cnt{{0, 1}};
    ll s = 0; int ans = 0;
    for (int x : a) {
        s += x;
        if (cnt.count(s - k)) ans += cnt[s - k];
        cnt[s]++;
    }
    return ans;
}

// Difference array: apply many range updates, then build
vector<int> diff(n + 1, 0);
for (auto& [l, r, v] : updates) { diff[l] += v; diff[r + 1] -= v; }
for (int i = 1; i < n; i++) diff[i] += diff[i - 1];   // diff[i] = final value at i
```

**Complexity:** O(n) to build, O(1) per query.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [303. Range Sum Query - Immutable](https://leetcode.com/problems/range-sum-query-immutable/) | Easy |
| 2 | [724. Find Pivot Index](https://leetcode.com/problems/find-pivot-index/) | Easy |
| 3 | [238. Product of Array Except Self](https://leetcode.com/problems/product-of-array-except-self/) | Medium |
| 4 | [560. Subarray Sum Equals K](https://leetcode.com/problems/subarray-sum-equals-k/) | Medium |
| 5 | [523. Continuous Subarray Sum](https://leetcode.com/problems/continuous-subarray-sum/) | Medium |
| 6 | [974. Subarray Sums Divisible by K](https://leetcode.com/problems/subarray-sums-divisible-by-k/) | Medium |
| 7 | [525. Contiguous Array](https://leetcode.com/problems/contiguous-array/) | Medium |
| 8 | [304. Range Sum Query 2D - Immutable](https://leetcode.com/problems/range-sum-query-2d-immutable/) | Medium |
| 9 | [1109. Corporate Flight Bookings](https://leetcode.com/problems/corporate-flight-bookings/) (difference array) | Medium |
| 10 | [1074. Number of Submatrices That Sum to Target](https://leetcode.com/problems/number-of-submatrices-that-sum-to-target/) | Hard |

---

## 1.3 Kadane's Algorithm (Maximum Subarray)

**Signals**
- "Maximum / minimum sum (or product) of a **contiguous** subarray".
- A best-so-far value where each element either *extends* the current run or *starts a new one*.

**Intuition**
At index i the best subarray **ending at i** is either `a[i]` alone or `a[i]` plus the best subarray ending at i-1.
If the running sum is negative it can only hurt what follows, so drop it and restart.
This is really a 1D DP where `dp[i] = max(a[i], dp[i-1] + a[i])` and you keep only the last value.

**Approach**
1. Keep `cur` (best sum ending here) and `best` (global best).
2. `cur = max(a[i], cur + a[i])`, then `best = max(best, cur)`.
3. **Product variant:** keep both `curMax` and `curMin`, because a negative number swaps them.
4. **Circular variant:** answer = `max(maxSubarray, total - minSubarray)`. If every element is negative, return `maxSubarray`.

**C++ template**
```cpp
int maxSubArray(vector<int>& a) {
    int cur = a[0], best = a[0];
    for (int i = 1; i < (int)a.size(); i++) {
        cur = max(a[i], cur + a[i]);
        best = max(best, cur);
    }
    return best;
}

int maxProduct(vector<int>& a) {
    // double: the running min can overflow int (even long long) although the answer fits in int
    double mx = a[0], mn = a[0], best = a[0];
    for (int i = 1; i < (int)a.size(); i++) {
        if (a[i] < 0) swap(mx, mn);
        mx = max((double)a[i], mx * a[i]);
        mn = min((double)a[i], mn * a[i]);
        best = max(best, mx);
    }
    return (int)best;
}
```

**Complexity:** O(n) time, O(1) space.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [121. Best Time to Buy and Sell Stock](https://leetcode.com/problems/best-time-to-buy-and-sell-stock/) | Easy |
| 2 | [53. Maximum Subarray](https://leetcode.com/problems/maximum-subarray/) | Medium |
| 3 | [152. Maximum Product Subarray](https://leetcode.com/problems/maximum-product-subarray/) | Medium |
| 4 | [918. Maximum Sum Circular Subarray](https://leetcode.com/problems/maximum-sum-circular-subarray/) | Medium |
| 5 | [1749. Maximum Absolute Sum of Any Subarray](https://leetcode.com/problems/maximum-absolute-sum-of-any-subarray/) | Medium |
| 6 | [978. Longest Turbulent Subarray](https://leetcode.com/problems/longest-turbulent-subarray/) | Medium |
| 7 | [2606. Find the Substring With Maximum Cost](https://leetcode.com/problems/find-the-substring-with-maximum-cost/) | Medium |
| 8 | [1186. Maximum Subarray Sum with One Deletion](https://leetcode.com/problems/maximum-subarray-sum-with-one-deletion/) | Medium |
| 9 | [1191. K-Concatenation Maximum Sum](https://leetcode.com/problems/k-concatenation-maximum-sum/) | Medium |
| 10 | [363. Max Sum of Rectangle No Larger Than K](https://leetcode.com/problems/max-sum-of-rectangle-no-larger-than-k/) | Hard |

---

## 1.4 Cyclic Sort / Index as Hash

**Signals**
- Array of size n holding numbers in range `[1..n]` or `[0..n]`.
- "Find missing / duplicate / first missing positive" in **O(1) extra space**.

**Intuition**
When values map directly to indices, each number has a "home": value `v` belongs at index `v-1`.
Swap every number into its home. Afterwards any index `i` that doesn't hold `i+1` reveals a missing or duplicate value.
Another option is to mark presence by negating `a[v-1]`, which uses the array itself as a hash set.

**Approach**
1. `i = 0`. While `i < n`: if `a[i]` is in range and `a[a[i]-1] != a[i]`, swap it to its home. Otherwise `i++`.
2. Scan again. The first `i` with `a[i] != i+1` gives the answer.
3. Each swap places one number permanently, so the total work is O(n) even though there's a while-loop inside.

**C++ template**
```cpp
int firstMissingPositive(vector<int>& a) {
    int n = a.size();
    for (int i = 0; i < n; ) {
        int v = a[i];
        if (v >= 1 && v <= n && a[v - 1] != v) swap(a[i], a[v - 1]);
        else i++;
    }
    for (int i = 0; i < n; i++) if (a[i] != i + 1) return i + 1;
    return n + 1;
}

// Negation marking: find all numbers that appear twice
vector<int> findDuplicates(vector<int>& a) {
    vector<int> res;
    for (int x : a) {
        int j = abs(x) - 1;
        if (a[j] < 0) res.push_back(abs(x));
        else a[j] = -a[j];
    }
    return res;
}
```

**Complexity:** O(n) time, O(1) extra space.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [268. Missing Number](https://leetcode.com/problems/missing-number/) | Easy |
| 2 | [448. Find All Numbers Disappeared in an Array](https://leetcode.com/problems/find-all-numbers-disappeared-in-an-array/) | Easy |
| 3 | [645. Set Mismatch](https://leetcode.com/problems/set-mismatch/) | Easy |
| 4 | [442. Find All Duplicates in an Array](https://leetcode.com/problems/find-all-duplicates-in-an-array/) | Medium |
| 5 | [287. Find the Duplicate Number](https://leetcode.com/problems/find-the-duplicate-number/) | Medium |
| 6 | [274. H-Index](https://leetcode.com/problems/h-index/) (counting sort) | Medium |
| 7 | [565. Array Nesting](https://leetcode.com/problems/array-nesting/) | Medium |
| 8 | [2471. Minimum Number of Operations to Sort a Binary Tree by Level](https://leetcode.com/problems/minimum-number-of-operations-to-sort-a-binary-tree-by-level/) (cycle decomposition) | Medium |
| 9 | [41. First Missing Positive](https://leetcode.com/problems/first-missing-positive/) | Hard |
| 10 | [765. Couples Holding Hands](https://leetcode.com/problems/couples-holding-hands/) | Hard |

---

## 1.5 Matrix & In-place Array Manipulation

**Signals**
- "Rotate / transpose / spiral / diagonal" a matrix, "do it **in place**".
- "Rotate an array by k", "next permutation", "set zeroes".

**Intuition**
Most of these are combinations of a few basic operations:
- **Rotate 90° clockwise** = transpose, then reverse each row.
- **Rotate array right by k** = reverse all, reverse the first k, reverse the rest.
- **Spiral** = keep four boundaries (`top, bottom, left, right`) and shrink them after each side.
- **In-place state change** (Game of Life, Set Zeroes): encode the new state in spare bits or in the first row/column so you don't need a copy.

**Approach**
1. Draw a 3×3 or 4×4 example and track where index `(i, j)` moves.
2. Break the transformation into reversals and transposes when you can.
3. For the boundary walk, check `top <= bottom && left <= right` before each side.

**C++ template**
```cpp
void rotate90(vector<vector<int>>& m) {
    int n = m.size();
    for (int i = 0; i < n; i++)
        for (int j = i + 1; j < n; j++) swap(m[i][j], m[j][i]);
    for (auto& row : m) reverse(row.begin(), row.end());
}

vector<int> spiralOrder(vector<vector<int>>& m) {
    vector<int> res;
    int top = 0, bot = m.size() - 1, l = 0, r = m[0].size() - 1;
    while (top <= bot && l <= r) {
        for (int j = l; j <= r; j++) res.push_back(m[top][j]);
        top++;
        for (int i = top; i <= bot; i++) res.push_back(m[i][r]);
        r--;
        if (top <= bot) { for (int j = r; j >= l; j--) res.push_back(m[bot][j]); bot--; }
        if (l <= r)     { for (int i = bot; i >= top; i--) res.push_back(m[i][l]); l++; }
    }
    return res;
}

void rotateArray(vector<int>& a, int k) {
    k %= a.size();
    reverse(a.begin(), a.end());
    reverse(a.begin(), a.begin() + k);
    reverse(a.begin() + k, a.end());
}
```

**Complexity:** O(n·m) time, O(1) extra space.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [867. Transpose Matrix](https://leetcode.com/problems/transpose-matrix/) | Easy |
| 2 | [566. Reshape the Matrix](https://leetcode.com/problems/reshape-the-matrix/) | Easy |
| 3 | [48. Rotate Image](https://leetcode.com/problems/rotate-image/) | Medium |
| 4 | [54. Spiral Matrix](https://leetcode.com/problems/spiral-matrix/) | Medium |
| 5 | [59. Spiral Matrix II](https://leetcode.com/problems/spiral-matrix-ii/) | Medium |
| 6 | [73. Set Matrix Zeroes](https://leetcode.com/problems/set-matrix-zeroes/) | Medium |
| 7 | [189. Rotate Array](https://leetcode.com/problems/rotate-array/) | Medium |
| 8 | [498. Diagonal Traverse](https://leetcode.com/problems/diagonal-traverse/) | Medium |
| 9 | [289. Game of Life](https://leetcode.com/problems/game-of-life/) | Medium |
| 10 | [31. Next Permutation](https://leetcode.com/problems/next-permutation/) | Medium |
