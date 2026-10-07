# 3. Binary Search

Binary search isn't only for finding a value in a sorted array. It works on **any monotonic yes/no
function**: if `check(x)` is `false, false, ..., false, true, true, ...`, binary search finds the first
`true` in O(log range) calls.

**One template to remember** (finds the first index where `ok(mid)` is true):
```cpp
int lo = LOW, hi = HIGH;          // the answer is guaranteed to be in [lo, hi]
while (lo < hi) {
    int mid = lo + (hi - lo) / 2;  // avoids overflow
    if (ok(mid)) hi = mid;         // mid might be the answer, so keep it
    else lo = mid + 1;             // mid is definitely not the answer
}
return lo;
```
For the **last** true in a `true, ..., true, false, ...` sequence, use `mid = lo + (hi - lo + 1) / 2`, `if (ok(mid)) lo = mid; else hi = mid - 1;`.
Rounding mid up prevents an infinite loop when `lo = mid`.

---

## 3.1 Classic Binary Search & Boundaries

**Signals**
- Sorted array or matrix, "find target / insert position / first and last occurrence".
- "O(log n) required".
- A peak or local structure where comparing `mid` with `mid+1` tells you which half to keep.

**Intuition**
Sortedness means one comparison at `mid` rules out half of the remaining range.
**Lower bound** = first index with `a[i] >= x`. **Upper bound** = first index with `a[i] > x`.
First and last occurrence = `lower_bound(x)` and `upper_bound(x) - 1`.

**Approach**
1. Write down the monotonic predicate, e.g. `a[mid] >= target`.
2. Pick the search range. Use `hi = n` (not `n-1`) when the answer can be "past the end".
3. Use the template above, then check the final `lo`.

**C++ template**
```cpp
int lowerBound(vector<int>& a, int x) {      // same as std::lower_bound
    int lo = 0, hi = a.size();
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;
        if (a[mid] >= x) hi = mid; else lo = mid + 1;
    }
    return lo;
}

vector<int> searchRange(vector<int>& a, int t) {
    int l = lower_bound(a.begin(), a.end(), t) - a.begin();
    if (l == (int)a.size() || a[l] != t) return {-1, -1};
    int r = upper_bound(a.begin(), a.end(), t) - a.begin() - 1;
    return {l, r};
}

int findPeakElement(vector<int>& a) {        // move toward the higher neighbour
    int lo = 0, hi = a.size() - 1;
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;
        if (a[mid] < a[mid + 1]) lo = mid + 1; else hi = mid;
    }
    return lo;
}

// Sorted 2D matrix (each row starts after the previous row ends): treat it as a 1D array
// index -> (idx / cols, idx % cols)
```

**Complexity:** O(log n).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [704. Binary Search](https://leetcode.com/problems/binary-search/) | Easy |
| 2 | [35. Search Insert Position](https://leetcode.com/problems/search-insert-position/) | Easy |
| 3 | [278. First Bad Version](https://leetcode.com/problems/first-bad-version/) | Easy |
| 4 | [374. Guess Number Higher or Lower](https://leetcode.com/problems/guess-number-higher-or-lower/) | Easy |
| 5 | [69. Sqrt(x)](https://leetcode.com/problems/sqrtx/) | Easy |
| 6 | [34. Find First and Last Position of Element in Sorted Array](https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/) | Medium |
| 7 | [74. Search a 2D Matrix](https://leetcode.com/problems/search-a-2d-matrix/) | Medium |
| 8 | [240. Search a 2D Matrix II](https://leetcode.com/problems/search-a-2d-matrix-ii/) (staircase from the top-right) | Medium |
| 9 | [162. Find Peak Element](https://leetcode.com/problems/find-peak-element/) | Medium |
| 10 | [540. Single Element in a Sorted Array](https://leetcode.com/problems/single-element-in-a-sorted-array/) | Medium |

---

## 3.2 Rotated / Modified Sorted Arrays & Partition Search

**Signals**
- "Sorted array **rotated** at an unknown pivot", "mountain array", "minimum in rotated array".
- "Median of two sorted arrays", "k closest", "kth missing".

**Intuition**
In a rotated sorted array, **at least one half around mid is always sorted**. Check whether the
target lies inside that sorted half's range. If it does, search there; if not, search the other half.
For the minimum, compare `a[mid]` with `a[hi]`: if `a[mid] > a[hi]`, the rotation point is to the right.

**Approach**
1. Work out which half is sorted: `a[lo] <= a[mid]` means the left half is sorted.
2. Check whether the target is inside that half's `[min, max]`, and discard the half that can't contain it.
3. With duplicates (problems 81 and 154), when `a[lo] == a[mid] == a[hi]` you can't tell which half is sorted, so shrink with `lo++` or `hi--`. The worst case becomes O(n).

**C++ template**
```cpp
int searchRotated(vector<int>& a, int t) {
    int lo = 0, hi = a.size() - 1;
    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (a[mid] == t) return mid;
        if (a[lo] <= a[mid]) {                        // left half sorted
            if (a[lo] <= t && t < a[mid]) hi = mid - 1;
            else lo = mid + 1;
        } else {                                      // right half sorted
            if (a[mid] < t && t <= a[hi]) lo = mid + 1;
            else hi = mid - 1;
        }
    }
    return -1;
}

int findMin(vector<int>& a) {
    int lo = 0, hi = a.size() - 1;
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;
        if (a[mid] > a[hi]) lo = mid + 1; else hi = mid;
    }
    return a[lo];
}
```

**Complexity:** O(log n). O(n) worst case with duplicates.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [1539. Kth Missing Positive Number](https://leetcode.com/problems/kth-missing-positive-number/) | Easy |
| 2 | [33. Search in Rotated Sorted Array](https://leetcode.com/problems/search-in-rotated-sorted-array/) | Medium |
| 3 | [153. Find Minimum in Rotated Sorted Array](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/) | Medium |
| 4 | [81. Search in Rotated Sorted Array II](https://leetcode.com/problems/search-in-rotated-sorted-array-ii/) | Medium |
| 5 | [852. Peak Index in a Mountain Array](https://leetcode.com/problems/peak-index-in-a-mountain-array/) | Medium |
| 6 | [658. Find K Closest Elements](https://leetcode.com/problems/find-k-closest-elements/) | Medium |
| 7 | [275. H-Index II](https://leetcode.com/problems/h-index-ii/) | Medium |
| 8 | [154. Find Minimum in Rotated Sorted Array II](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array-ii/) | Hard |
| 9 | [1095. Find in Mountain Array](https://leetcode.com/problems/find-in-mountain-array/) | Hard |
| 10 | [4. Median of Two Sorted Arrays](https://leetcode.com/problems/median-of-two-sorted-arrays/) (binary search on the partition) | Hard |

---

## 3.3 Binary Search on the Answer

**Signals**
- "**Minimise the maximum**...", "**maximise the minimum**...", "minimum speed / capacity / days / time so that...".
- The answer is a number in a known range `[lo, hi]`, and you can easily answer *"is X good enough?"*.
- Huge value ranges (up to 10⁹) but a small n.

**Intuition**
Finding the optimum directly is hard, but **checking** a candidate is easy (usually a greedy O(n) pass).
Feasibility is monotonic: if speed X works, any speed > X also works. So binary search on X, and the
first feasible X is the answer.

**Approach**
1. Define the answer range: `lo` = smallest possible (often `max(a)` or 1), `hi` = largest possible (often `sum(a)` or `max(a)`).
2. Write `bool feasible(X)`, usually a greedy simulation.
3. Binary search for the first feasible X (minimise) or the last feasible X (maximise).

**C++ template**
```cpp
// Koko Eating Bananas: minimum speed to finish within h hours
int minEatingSpeed(vector<int>& piles, int h) {
    auto feasible = [&](int k) {
        long long hours = 0;
        for (int p : piles) hours += (p + k - 1) / k;   // ceil(p / k)
        return hours <= h;
    };
    int lo = 1, hi = *max_element(piles.begin(), piles.end());
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;
        if (feasible(mid)) hi = mid; else lo = mid + 1;
    }
    return lo;
}

// Split Array Largest Sum / Ship Within D Days: minimise the maximum group sum
int splitArray(vector<int>& a, int k) {
    auto groupsNeeded = [&](long long cap) {
        int groups = 1; long long cur = 0;
        for (int x : a) {
            if (cur + x > cap) { groups++; cur = 0; }
            cur += x;
        }
        return groups;
    };
    long long lo = *max_element(a.begin(), a.end()), hi = accumulate(a.begin(), a.end(), 0LL);
    while (lo < hi) {
        long long mid = lo + (hi - lo) / 2;
        if (groupsNeeded(mid) <= k) hi = mid; else lo = mid + 1;
    }
    return lo;
}
```

**Complexity:** O(n · log(range)).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [875. Koko Eating Bananas](https://leetcode.com/problems/koko-eating-bananas/) | Medium |
| 2 | [1011. Capacity To Ship Packages Within D Days](https://leetcode.com/problems/capacity-to-ship-packages-within-d-days/) | Medium |
| 3 | [1283. Find the Smallest Divisor Given a Threshold](https://leetcode.com/problems/find-the-smallest-divisor-given-a-threshold/) | Medium |
| 4 | [1482. Minimum Number of Days to Make m Bouquets](https://leetcode.com/problems/minimum-number-of-days-to-make-m-bouquets/) | Medium |
| 5 | [2187. Minimum Time to Complete Trips](https://leetcode.com/problems/minimum-time-to-complete-trips/) | Medium |
| 6 | [1552. Magnetic Force Between Two Balls](https://leetcode.com/problems/magnetic-force-between-two-balls/) (maximise the minimum) | Medium |
| 7 | [2064. Minimized Maximum of Products Distributed to Any Store](https://leetcode.com/problems/minimized-maximum-of-products-distributed-to-any-store/) | Medium |
| 8 | [410. Split Array Largest Sum](https://leetcode.com/problems/split-array-largest-sum/) | Hard |
| 9 | [668. Kth Smallest Number in Multiplication Table](https://leetcode.com/problems/kth-smallest-number-in-multiplication-table/) | Hard |
| 10 | [719. Find K-th Smallest Pair Distance](https://leetcode.com/problems/find-k-th-smallest-pair-distance/) | Hard |
