# 8. Heaps, Greedy & Intervals

```cpp
priority_queue<int> maxHeap;                                  // largest on top
priority_queue<int, vector<int>, greater<int>> minHeap;       // smallest on top
// Custom comparator: the lambda returns true when a should sit BELOW b
auto cmp = [](const pii& a, const pii& b) { return a.second > b.second; };   // min-heap on .second
priority_queue<pii, vector<pii>, decltype(cmp)> pq(cmp);
```

---

## 8.1 Top-K Elements

**Signals**
- "Kth largest / smallest", "top k frequent", "k closest points", "repeatedly take the largest" (stones, gifts).
- "Rearrange so no two adjacent elements are equal" (greedy on the most frequent remaining item).

**Intuition**
For the **k largest**, keep a **min-heap of size k**. Its top is the smallest of the current top-k, so any
new element bigger than the top replaces it. That costs O(n log k) instead of O(n log n) for a full sort.
For "repeatedly take the max", a max-heap gives the current max in O(log n) after every change.
(Quickselect gives O(n) on average for a single kth element.)

**C++ template**
```cpp
int findKthLargest(vector<int>& a, int k) {
    priority_queue<int, vector<int>, greater<int>> pq;       // min-heap of size k
    for (int x : a) {
        pq.push(x);
        if ((int)pq.size() > k) pq.pop();
    }
    return pq.top();
}

vector<int> topKFrequent(vector<int>& a, int k) {
    unordered_map<int,int> f; for (int x : a) f[x]++;
    priority_queue<pii, vector<pii>, greater<pii>> pq;       // (freq, value)
    for (auto& [v, c] : f) { pq.push({c, v}); if ((int)pq.size() > k) pq.pop(); }
    vector<int> res;
    while (!pq.empty()) { res.push_back(pq.top().second); pq.pop(); }
    return res;
}

// Reorganize String: always place the most frequent character that differs from the last one placed
string reorganizeString(string s) {
    vector<int> f(26); for (char c : s) f[c - 'a']++;
    priority_queue<pii> pq;
    for (int i = 0; i < 26; i++) if (f[i]) pq.push({f[i], i});
    string res; pii prev = {0, -1};
    while (!pq.empty()) {
        auto [c, ch] = pq.top(); pq.pop();
        res += char('a' + ch);
        if (prev.first > 0) pq.push(prev);
        prev = {c - 1, ch};
    }
    return res.size() == s.size() ? res : "";
}
```

**Complexity:** O(n log k).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [703. Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/) | Easy |
| 2 | [1046. Last Stone Weight](https://leetcode.com/problems/last-stone-weight/) | Easy |
| 3 | [215. Kth Largest Element in an Array](https://leetcode.com/problems/kth-largest-element-in-an-array/) | Medium |
| 4 | [973. K Closest Points to Origin](https://leetcode.com/problems/k-closest-points-to-origin/) | Medium |
| 5 | [692. Top K Frequent Words](https://leetcode.com/problems/top-k-frequent-words/) | Medium |
| 6 | [451. Sort Characters By Frequency](https://leetcode.com/problems/sort-characters-by-frequency/) | Medium |
| 7 | [1962. Remove Stones to Minimize the Total](https://leetcode.com/problems/remove-stones-to-minimize-the-total/) | Medium |
| 8 | [767. Reorganize String](https://leetcode.com/problems/reorganize-string/) | Medium |
| 9 | [621. Task Scheduler](https://leetcode.com/problems/task-scheduler/) | Medium |
| 10 | [1405. Longest Happy String](https://leetcode.com/problems/longest-happy-string/) | Medium |

---

## 8.2 Two Heaps & Scheduling with Heaps

**Signals**
- "Running median", "sliding window median".
- "Choose projects to maximise capital" (IPO), "assign tasks to servers", "meeting rooms", "hire k workers with the best ratio".

**Intuition**
**Median:** split the numbers into a **max-heap (smaller half)** and a **min-heap (larger half)**, with sizes
differing by at most 1. The median is at one or both tops.
**Scheduling:** sort events by time. One heap holds what is *available now* (sorted by priority) and another
holds what is *busy* (sorted by free-up time). Move items between them as time passes.

**C++ template**
```cpp
class MedianFinder {
    priority_queue<int> lo;                                  // max-heap: smaller half
    priority_queue<int, vector<int>, greater<int>> hi;       // min-heap: larger half
public:
    void addNum(int x) {
        lo.push(x);
        hi.push(lo.top()); lo.pop();                         // keep every value in lo <= every value in hi
        if (hi.size() > lo.size()) { lo.push(hi.top()); hi.pop(); }
    }
    double findMedian() {
        return lo.size() > hi.size() ? lo.top() : (lo.top() + (double)hi.top()) / 2;
    }
};

// IPO: projects you can afford go into a max-heap of profits
int findMaximizedCapital(int k, int w, vector<int>& profits, vector<int>& capital) {
    vector<pii> p; for (int i = 0; i < (int)profits.size(); i++) p.push_back({capital[i], profits[i]});
    sort(p.begin(), p.end());
    priority_queue<int> avail; int i = 0;
    while (k--) {
        while (i < (int)p.size() && p[i].first <= w) avail.push(p[i++].second);
        if (avail.empty()) break;
        w += avail.top(); avail.pop();
    }
    return w;
}
```

**Complexity:** O(log n) per insert, O(n log n) overall.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [1834. Single-Threaded CPU](https://leetcode.com/problems/single-threaded-cpu/) | Medium |
| 2 | [1882. Process Tasks Using Servers](https://leetcode.com/problems/process-tasks-using-servers/) | Medium |
| 3 | [1942. The Number of the Smallest Unoccupied Chair](https://leetcode.com/problems/the-number-of-the-smallest-unoccupied-chair/) | Medium |
| 4 | [2462. Total Cost to Hire K Workers](https://leetcode.com/problems/total-cost-to-hire-k-workers/) | Medium |
| 5 | [2542. Maximum Subsequence Score](https://leetcode.com/problems/maximum-subsequence-score/) | Medium |
| 6 | [295. Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/) | Hard |
| 7 | [480. Sliding Window Median](https://leetcode.com/problems/sliding-window-median/) | Hard |
| 8 | [502. IPO](https://leetcode.com/problems/ipo/) | Hard |
| 9 | [857. Minimum Cost to Hire K Workers](https://leetcode.com/problems/minimum-cost-to-hire-k-workers/) | Hard |
| 10 | [2402. Meeting Rooms III](https://leetcode.com/problems/meeting-rooms-iii/) | Hard |

---

## 8.3 K-way Merge

**Signals**
- "Merge k sorted lists / arrays", "kth smallest in a sorted matrix", "k pairs with the smallest sums", "smallest range covering k lists".
- Ugly numbers: several sorted streams generated from one another.

**Intuition**
The next smallest element overall must be the head of one of the k lists. Keep the k heads in a **min-heap**:
pop the smallest, then push the next element from the same list. Each step costs O(log k).

**C++ template**
```cpp
ListNode* mergeKLists(vector<ListNode*>& lists) {
    auto cmp = [](ListNode* a, ListNode* b) { return a->val > b->val; };
    priority_queue<ListNode*, vector<ListNode*>, decltype(cmp)> pq(cmp);
    for (auto l : lists) if (l) pq.push(l);
    ListNode dummy, *tail = &dummy;
    while (!pq.empty()) {
        ListNode* n = pq.top(); pq.pop();
        tail->next = n; tail = n;
        if (n->next) pq.push(n->next);
    }
    return dummy.next;
}

// Kth smallest in a sorted matrix: heap of (value, row, col), start with column 0 of each row
int kthSmallest(vector<vector<int>>& m, int k) {
    using T = tuple<int,int,int>;
    priority_queue<T, vector<T>, greater<T>> pq;
    for (int r = 0; r < (int)m.size(); r++) pq.push({m[r][0], r, 0});
    while (--k) {
        auto [v, r, c] = pq.top(); pq.pop();
        if (c + 1 < (int)m[0].size()) pq.push({m[r][c + 1], r, c + 1});
    }
    return get<0>(pq.top());
}
```

**Complexity:** O(N log k), where N = total number of elements.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [88. Merge Sorted Array](https://leetcode.com/problems/merge-sorted-array/) (k = 2 warm-up) | Easy |
| 2 | [264. Ugly Number II](https://leetcode.com/problems/ugly-number-ii/) | Medium |
| 3 | [313. Super Ugly Number](https://leetcode.com/problems/super-ugly-number/) | Medium |
| 4 | [378. Kth Smallest Element in a Sorted Matrix](https://leetcode.com/problems/kth-smallest-element-in-a-sorted-matrix/) | Medium |
| 5 | [373. Find K Pairs with Smallest Sums](https://leetcode.com/problems/find-k-pairs-with-smallest-sums/) | Medium |
| 6 | [786. K-th Smallest Prime Fraction](https://leetcode.com/problems/k-th-smallest-prime-fraction/) | Medium |
| 7 | [1508. Range Sum of Sorted Subarray Sums](https://leetcode.com/problems/range-sum-of-sorted-subarray-sums/) | Medium |
| 8 | [23. Merge k Sorted Lists](https://leetcode.com/problems/merge-k-sorted-lists/) | Hard |
| 9 | [632. Smallest Range Covering Elements from K Lists](https://leetcode.com/problems/smallest-range-covering-elements-from-k-lists/) | Hard |
| 10 | [1439. Find the Kth Smallest Sum of a Matrix With Sorted Rows](https://leetcode.com/problems/find-the-kth-smallest-sum-of-a-matrix-with-sorted-rows/) | Hard |

---

## 8.4 Intervals

**Signals**
- Pairs `[start, end]`: "merge overlapping", "insert", "minimum to remove so none overlap", "arrows to burst balloons", "how many rooms or cars at once".

**Intuition**
**Sort first.** Then:
- **Merge:** sort by start; if the next start ≤ the current end, extend the end, otherwise start a new interval.
- **Max non-overlapping / min removals / min arrows:** sort by **end** and greedily keep the interval that ends earliest, because it leaves the most room for the rest (the activity selection proof).
- **Max overlap at any moment (rooms):** a sweep line with `+1` at each start and `-1` at each end, or a min-heap of end times.

Two intervals `[a, b]` and `[c, d]` overlap iff `max(a, c) <= min(b, d)`.

**C++ template**
```cpp
vector<vector<int>> merge(vector<vector<int>>& iv) {
    sort(iv.begin(), iv.end());
    vector<vector<int>> res;
    for (auto& x : iv) {
        if (res.empty() || res.back()[1] < x[0]) res.push_back(x);
        else res.back()[1] = max(res.back()[1], x[1]);
    }
    return res;
}

int eraseOverlapIntervals(vector<vector<int>>& iv) {           // min removals
    sort(iv.begin(), iv.end(), [](auto& a, auto& b) { return a[1] < b[1]; });
    int keep = 0, end = INT_MIN;
    for (auto& x : iv) if (x[0] >= end) { keep++; end = x[1]; }
    return iv.size() - keep;
}

int minRooms(vector<vector<int>>& iv) {                        // sweep line
    map<int,int> delta;
    for (auto& x : iv) { delta[x[0]]++; delta[x[1]]--; }
    int cur = 0, best = 0;
    for (auto& [t, d] : delta) { cur += d; best = max(best, cur); }
    return best;
}
```

**Complexity:** O(n log n).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [56. Merge Intervals](https://leetcode.com/problems/merge-intervals/) | Medium |
| 2 | [57. Insert Interval](https://leetcode.com/problems/insert-interval/) | Medium |
| 3 | [435. Non-overlapping Intervals](https://leetcode.com/problems/non-overlapping-intervals/) | Medium |
| 4 | [452. Minimum Number of Arrows to Burst Balloons](https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/) | Medium |
| 5 | [986. Interval List Intersections](https://leetcode.com/problems/interval-list-intersections/) | Medium |
| 6 | [1288. Remove Covered Intervals](https://leetcode.com/problems/remove-covered-intervals/) | Medium |
| 7 | [1094. Car Pooling](https://leetcode.com/problems/car-pooling/) | Medium |
| 8 | [729. My Calendar I](https://leetcode.com/problems/my-calendar-i/) | Medium |
| 9 | [2406. Divide Intervals Into Minimum Number of Groups](https://leetcode.com/problems/divide-intervals-into-minimum-number-of-groups/) | Medium |
| 10 | [1851. Minimum Interval to Include Each Query](https://leetcode.com/problems/minimum-interval-to-include-each-query/) | Hard |

---

## 8.5 Greedy

**Signals**
- Picking the best local option looks like it works, and there is **no need to revisit** earlier choices.
- "Minimum number of jumps / arrows / coins (canonical coin systems)", "can you reach", "assign to maximise satisfied", "gas station".
- Usually an O(n) or O(n log n) answer is expected.

**Intuition**
Greedy works when a **locally optimal choice is part of some globally optimal solution**. You can usually
prove it with an **exchange argument**: take any optimal solution and swap in the greedy choice without making it worse.
If you can find a counterexample where greedy fails, switch to DP.

**Common greedy moves**
- **Sort first**, by end time, by ratio, or by size.
- **Track the farthest reach** (Jump Game).
- **Reset when the running total goes negative** (Gas Station, Kadane).
- **Two passes**, left to right and right to left, for neighbour constraints (Candy).
- **Match smallest to smallest** (Assign Cookies).

**C++ template**
```cpp
bool canJump(vector<int>& a) {
    int reach = 0;
    for (int i = 0; i < (int)a.size(); i++) {
        if (i > reach) return false;
        reach = max(reach, i + a[i]);
    }
    return true;
}

int jump(vector<int>& a) {                     // BFS by levels: [curEnd] is one jump's range
    int jumps = 0, curEnd = 0, far = 0;
    for (int i = 0; i < (int)a.size() - 1; i++) {
        far = max(far, i + a[i]);
        if (i == curEnd) { jumps++; curEnd = far; }
    }
    return jumps;
}

int canCompleteCircuit(vector<int>& gas, vector<int>& cost) {
    int total = 0, tank = 0, start = 0;
    for (int i = 0; i < (int)gas.size(); i++) {
        total += gas[i] - cost[i]; tank += gas[i] - cost[i];
        if (tank < 0) { start = i + 1; tank = 0; }   // no start in [old start..i] can work
    }
    return total < 0 ? -1 : start;
}

int candy(vector<int>& r) {
    int n = r.size(); vector<int> c(n, 1);
    for (int i = 1; i < n; i++) if (r[i] > r[i - 1]) c[i] = c[i - 1] + 1;
    for (int i = n - 2; i >= 0; i--) if (r[i] > r[i + 1]) c[i] = max(c[i], c[i + 1] + 1);
    return accumulate(c.begin(), c.end(), 0);
}
```

**Complexity:** usually O(n) or O(n log n).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [455. Assign Cookies](https://leetcode.com/problems/assign-cookies/) | Easy |
| 2 | [55. Jump Game](https://leetcode.com/problems/jump-game/) | Medium |
| 3 | [45. Jump Game II](https://leetcode.com/problems/jump-game-ii/) | Medium |
| 4 | [134. Gas Station](https://leetcode.com/problems/gas-station/) | Medium |
| 5 | [763. Partition Labels](https://leetcode.com/problems/partition-labels/) | Medium |
| 6 | [846. Hand of Straights](https://leetcode.com/problems/hand-of-straights/) | Medium |
| 7 | [678. Valid Parenthesis String](https://leetcode.com/problems/valid-parenthesis-string/) | Medium |
| 8 | [1899. Merge Triplets to Form Target Triplet](https://leetcode.com/problems/merge-triplets-to-form-target-triplet/) | Medium |
| 9 | [135. Candy](https://leetcode.com/problems/candy/) | Hard |
| 10 | [330. Patching Array](https://leetcode.com/problems/patching-array/) | Hard |
