# 2. Two Pointers & Sliding Window

Both patterns replace an O(n²) "try every pair or every subarray" loop with O(n) by **moving pointers
in one direction only**. That only works when you can safely throw away a whole set of candidates at
each step, which usually comes from **sortedness** (two pointers) or **monotonicity** (sliding window:
growing a window never makes a sum smaller).

---

## 2.1 Two Pointers: Opposite Ends

**Signals**
- **Sorted** array (or you're allowed to sort), "find a pair / triplet with sum X".
- Palindrome check, "container", "trap water", reversing in place.

**Intuition**
In a sorted array with `L` at the start and `R` at the end:
- if `a[L] + a[R] < target`, then pairing `a[L]` with anything to the left of R is even smaller, so `a[L]` is useless and you move `L++`.
- if the sum is too large, `a[R]` is useless, so `R--`.

Each step removes one candidate for good, so the scan is O(n).
For k-Sum, fix the first k-2 elements with loops and run two pointers on the rest.

**Approach**
1. Sort if needed. Set `L = 0`, `R = n-1`.
2. Compare, then move the pointer whose element can't be part of a better answer.
3. For 3Sum, skip duplicates: `if (i > 0 && a[i] == a[i-1]) continue;`, and after a match skip equal values on both sides.
4. Container / Trap Water: always move the **shorter** side, because the shorter wall limits the answer.

**C++ template**
```cpp
vector<vector<int>> threeSum(vector<int>& a) {
    sort(a.begin(), a.end());
    vector<vector<int>> res;
    int n = a.size();
    for (int i = 0; i < n - 2; i++) {
        if (i > 0 && a[i] == a[i - 1]) continue;
        int l = i + 1, r = n - 1;
        while (l < r) {
            int s = a[i] + a[l] + a[r];
            if (s < 0) l++;
            else if (s > 0) r--;
            else {
                res.push_back({a[i], a[l], a[r]});
                while (l < r && a[l] == a[l + 1]) l++;
                while (l < r && a[r] == a[r - 1]) r--;
                l++; r--;
            }
        }
    }
    return res;
}

int trap(vector<int>& h) {
    int l = 0, r = h.size() - 1, lmax = 0, rmax = 0, water = 0;
    while (l < r) {
        if (h[l] < h[r]) { lmax = max(lmax, h[l]); water += lmax - h[l]; l++; }
        else             { rmax = max(rmax, h[r]); water += rmax - h[r]; r--; }
    }
    return water;
}
```

**Complexity:** O(n) per pass, plus O(n log n) if you sort. 3Sum is O(n²).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [125. Valid Palindrome](https://leetcode.com/problems/valid-palindrome/) | Easy |
| 2 | [977. Squares of a Sorted Array](https://leetcode.com/problems/squares-of-a-sorted-array/) | Easy |
| 3 | [680. Valid Palindrome II](https://leetcode.com/problems/valid-palindrome-ii/) | Easy |
| 4 | [167. Two Sum II - Input Array Is Sorted](https://leetcode.com/problems/two-sum-ii-input-array-is-sorted/) | Medium |
| 5 | [15. 3Sum](https://leetcode.com/problems/3sum/) | Medium |
| 6 | [16. 3Sum Closest](https://leetcode.com/problems/3sum-closest/) | Medium |
| 7 | [18. 4Sum](https://leetcode.com/problems/4sum/) | Medium |
| 8 | [11. Container With Most Water](https://leetcode.com/problems/container-with-most-water/) | Medium |
| 9 | [881. Boats to Save People](https://leetcode.com/problems/boats-to-save-people/) | Medium |
| 10 | [42. Trapping Rain Water](https://leetcode.com/problems/trapping-rain-water/) | Hard |

---

## 2.2 Two Pointers: Same Direction (Read / Write, Partitioning)

**Signals**
- "Remove / move elements **in place**", "return new length", "keep at most k duplicates".
- "Merge two sorted arrays", "is subsequence", "sort 0s, 1s and 2s".

**Intuition**
A **read** pointer looks at every element, and a **write** pointer marks where the next *kept* element goes.
Everything before `write` is the finished answer, so you build the output inside the input array.
Dutch National Flag uses three pointers (`low`, `mid`, `high`) to split the array into three regions in one pass.

**Approach**
1. `w = 0`. For each `r`: if `a[r]` should be kept, set `a[w++] = a[r]`.
2. "Keep at most k copies" in a sorted array: keep `a[r]` if `w < k || a[r] != a[w-k]`.
3. Merging sorted arrays in place: fill **from the back** so you don't overwrite unread values.

**C++ template**
```cpp
int removeDuplicatesAtMostK(vector<int>& a, int k) {
    int w = 0;
    for (int x : a)
        if (w < k || x != a[w - k]) a[w++] = x;
    return w;
}

void sortColors(vector<int>& a) {            // Dutch National Flag
    int lo = 0, mid = 0, hi = a.size() - 1;
    while (mid <= hi) {
        if (a[mid] == 0) swap(a[lo++], a[mid++]);
        else if (a[mid] == 1) mid++;
        else swap(a[mid], a[hi--]);          // don't advance mid: the swapped-in value is unchecked
    }
}

void merge(vector<int>& a, int m, vector<int>& b, int n) {
    int i = m - 1, j = n - 1, k = m + n - 1;
    while (j >= 0) a[k--] = (i >= 0 && a[i] > b[j]) ? a[i--] : b[j--];
}
```

**Complexity:** O(n) time, O(1) space.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [26. Remove Duplicates from Sorted Array](https://leetcode.com/problems/remove-duplicates-from-sorted-array/) | Easy |
| 2 | [27. Remove Element](https://leetcode.com/problems/remove-element/) | Easy |
| 3 | [283. Move Zeroes](https://leetcode.com/problems/move-zeroes/) | Easy |
| 4 | [88. Merge Sorted Array](https://leetcode.com/problems/merge-sorted-array/) | Easy |
| 5 | [392. Is Subsequence](https://leetcode.com/problems/is-subsequence/) | Easy |
| 6 | [905. Sort Array By Parity](https://leetcode.com/problems/sort-array-by-parity/) | Easy |
| 7 | [844. Backspace String Compare](https://leetcode.com/problems/backspace-string-compare/) | Easy |
| 8 | [80. Remove Duplicates from Sorted Array II](https://leetcode.com/problems/remove-duplicates-from-sorted-array-ii/) | Medium |
| 9 | [75. Sort Colors](https://leetcode.com/problems/sort-colors/) | Medium |
| 10 | [443. String Compression](https://leetcode.com/problems/string-compression/) | Medium |

---

## 2.3 Sliding Window: Fixed Size

**Signals**
- "Subarray / substring of **size k**": max sum, average, count, contains a permutation or anagram.

**Intuition**
Two neighbouring windows share k-1 elements. Instead of recomputing the whole window, **add the element
coming in and remove the element going out**, which makes each step O(1).

**Approach**
1. Build the first window over `[0, k-1]`.
2. For `i = k..n-1`: add `a[i]`, remove `a[i-k]`, then update the answer.
3. For anagram or permutation problems, keep a `cnt[26]` difference and a `matches` counter so each comparison is O(1).

**C++ template**
```cpp
double findMaxAverage(vector<int>& a, int k) {
    long long sum = 0;
    for (int i = 0; i < k; i++) sum += a[i];
    long long best = sum;
    for (int i = k; i < (int)a.size(); i++) {
        sum += a[i] - a[i - k];
        best = max(best, sum);
    }
    return (double)best / k;
}

// Find all anagrams of p in s
vector<int> findAnagrams(string s, string p) {
    vector<int> res, need(26, 0), win(26, 0);
    int k = p.size();
    if ((int)s.size() < k) return res;
    for (char c : p) need[c - 'a']++;
    for (int i = 0; i < (int)s.size(); i++) {
        win[s[i] - 'a']++;
        if (i >= k) win[s[i - k] - 'a']--;
        if (i >= k - 1 && win == need) res.push_back(i - k + 1);
    }
    return res;
}
```

**Complexity:** O(n) time (O(26n) for the vector comparison).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [643. Maximum Average Subarray I](https://leetcode.com/problems/maximum-average-subarray-i/) | Easy |
| 2 | [219. Contains Duplicate II](https://leetcode.com/problems/contains-duplicate-ii/) | Easy |
| 3 | [1456. Maximum Number of Vowels in a Substring of Given Length](https://leetcode.com/problems/maximum-number-of-vowels-in-a-substring-of-given-length/) | Medium |
| 4 | [1343. Number of Sub-arrays of Size K and Average Greater than or Equal to Threshold](https://leetcode.com/problems/number-of-sub-arrays-of-size-k-and-average-greater-than-or-equal-to-threshold/) | Medium |
| 5 | [567. Permutation in String](https://leetcode.com/problems/permutation-in-string/) | Medium |
| 6 | [438. Find All Anagrams in a String](https://leetcode.com/problems/find-all-anagrams-in-a-string/) | Medium |
| 7 | [1423. Maximum Points You Can Obtain from Cards](https://leetcode.com/problems/maximum-points-you-can-obtain-from-cards/) | Medium |
| 8 | [1052. Grumpy Bookstore Owner](https://leetcode.com/problems/grumpy-bookstore-owner/) | Medium |
| 9 | [2461. Maximum Sum of Distinct Subarrays With Length K](https://leetcode.com/problems/maximum-sum-of-distinct-subarrays-with-length-k/) | Medium |
| 10 | [239. Sliding Window Maximum](https://leetcode.com/problems/sliding-window-maximum/) (needs a deque, see 4.3) | Hard |

---

## 2.4 Sliding Window: Variable Size

**Signals**
- "**Longest / shortest** contiguous subarray or substring such that...", "at most K distinct", "without repeating", "sum ≥ target".
- All numbers are non-negative, or the condition is **monotonic**: if a window is valid, every smaller window inside it is valid too (or every larger one is).

**Intuition**
Expand `R` to include more. When the window becomes **invalid**, shrink `L` until it's valid again.
Because the condition is monotonic, a valid window at `L` never needs to look at an earlier `L` again,
so both pointers only move forward, giving O(n) total.

**"Exactly K" trick:** `exactly(K) = atMost(K) - atMost(K-1)`.

**Approach (longest window)**
1. `L = 0`. For each `R`: add `a[R]` to the window state (map, count, sum).
2. `while (window invalid)`: remove `a[L]`, then `L++`.
3. Update `ans = max(ans, R - L + 1)`.

**Approach (shortest window)**
1. For each `R`: add `a[R]`.
2. `while (window valid)`: update `ans = min(ans, R - L + 1)`, remove `a[L]`, then `L++`.

**C++ template**
```cpp
// Longest substring without repeating characters
int lengthOfLongestSubstring(string s) {
    vector<int> cnt(256, 0);
    int L = 0, ans = 0;
    for (int R = 0; R < (int)s.size(); R++) {
        cnt[s[R]]++;
        while (cnt[s[R]] > 1) cnt[s[L++]]--;
        ans = max(ans, R - L + 1);
    }
    return ans;
}

// Minimum window substring (shortest valid window)
string minWindow(string s, string t) {
    vector<int> need(128, 0);
    for (char c : t) need[c]++;
    int missing = t.size(), L = 0, bestL = 0, bestLen = INT_MAX;
    for (int R = 0; R < (int)s.size(); R++) {
        if (need[s[R]]-- > 0) missing--;
        while (missing == 0) {
            if (R - L + 1 < bestLen) { bestLen = R - L + 1; bestL = L; }
            if (++need[s[L++]] > 0) missing++;
        }
    }
    return bestLen == INT_MAX ? "" : s.substr(bestL, bestLen);
}

// Count subarrays with at most K distinct values (use for the "exactly K" trick)
int atMost(vector<int>& a, int k) {
    unordered_map<int,int> cnt; int L = 0, res = 0;
    for (int R = 0; R < (int)a.size(); R++) {
        if (cnt[a[R]]++ == 0) k--;
        while (k < 0) if (--cnt[a[L++]] == 0) k++;
        res += R - L + 1;          // number of valid subarrays ending at R
    }
    return res;
}
```

**Complexity:** O(n) time, O(alphabet) or O(n) space.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [3. Longest Substring Without Repeating Characters](https://leetcode.com/problems/longest-substring-without-repeating-characters/) | Medium |
| 2 | [209. Minimum Size Subarray Sum](https://leetcode.com/problems/minimum-size-subarray-sum/) | Medium |
| 3 | [1004. Max Consecutive Ones III](https://leetcode.com/problems/max-consecutive-ones-iii/) | Medium |
| 4 | [1493. Longest Subarray of 1's After Deleting One Element](https://leetcode.com/problems/longest-subarray-of-1s-after-deleting-one-element/) | Medium |
| 5 | [904. Fruit Into Baskets](https://leetcode.com/problems/fruit-into-baskets/) | Medium |
| 6 | [424. Longest Repeating Character Replacement](https://leetcode.com/problems/longest-repeating-character-replacement/) | Medium |
| 7 | [713. Subarray Product Less Than K](https://leetcode.com/problems/subarray-product-less-than-k/) | Medium |
| 8 | [1248. Count Number of Nice Subarrays](https://leetcode.com/problems/count-number-of-nice-subarrays/) (exactly-K trick) | Medium |
| 9 | [76. Minimum Window Substring](https://leetcode.com/problems/minimum-window-substring/) | Hard |
| 10 | [992. Subarrays with K Different Integers](https://leetcode.com/problems/subarrays-with-k-different-integers/) | Hard |
