# 4. Stack & Queue

A **stack** (LIFO) fits whenever the *most recent unresolved* item is the one to deal with next:
brackets, nested expressions, undo, and "nearest greater to the left".
A **queue** (FIFO) handles things in arrival order. A **deque** lets you also throw away stale items
from the back, which gives the monotonic queue.

---

## 4.1 Stack: Matching, Nesting & Expression Evaluation

**Signals**
- Brackets, "valid parentheses", "decode `3[a2[c]]`", "simplify path", "calculator", "asteroid collision".
- Something opens and must later be closed or cancelled **in reverse order**.

**Intuition**
Nested structure means the innermost (most recently opened) part has to be resolved first, and that is
exactly what a stack does. For expressions, keep a stack of numbers and apply the pending operator when
you see the next one. Precedence decides whether you combine immediately (`*`, `/`) or push for later (`+`, `-`).

**Approach**
1. Push "openers" or partial state.
2. On a "closer", pop and check that it matches, or combine with what you popped.
3. At the end the stack must be empty (for validity), or it holds the pieces of the answer.
4. For nested decoding (`k[...]`), push `(currentString, k)` on `[`, then on `]` pop and set `prev + cur * k`.

**C++ template**
```cpp
bool isValid(string s) {
    stack<char> st;
    for (char c : s) {
        if (c == '(' || c == '[' || c == '{') st.push(c);
        else {
            if (st.empty()) return false;
            char o = st.top(); st.pop();
            if ((c == ')' && o != '(') || (c == ']' && o != '[') || (c == '}' && o != '{')) return false;
        }
    }
    return st.empty();
}

// Basic Calculator II: + - * / without parentheses
int calculate(string s) {
    vector<long long> st; long long num = 0; char op = '+';
    for (int i = 0; i < (int)s.size(); i++) {
        char c = s[i];
        if (isdigit(c)) num = num * 10 + (c - '0');
        if ((!isdigit(c) && c != ' ') || i == (int)s.size() - 1) {
            if (op == '+') st.push_back(num);
            else if (op == '-') st.push_back(-num);
            else if (op == '*') st.back() *= num;
            else st.back() /= num;
            op = c; num = 0;
        }
    }
    return accumulate(st.begin(), st.end(), 0LL);
}

// Min stack: store (value, minimum so far) pairs
stack<pair<int,int>> ms;
void push(int x) { ms.push({x, ms.empty() ? x : min(x, ms.top().second)}); }
```

**Complexity:** O(n) time, O(n) space.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [20. Valid Parentheses](https://leetcode.com/problems/valid-parentheses/) | Easy |
| 2 | [1047. Remove All Adjacent Duplicates In String](https://leetcode.com/problems/remove-all-adjacent-duplicates-in-string/) | Easy |
| 3 | [155. Min Stack](https://leetcode.com/problems/min-stack/) | Medium |
| 4 | [150. Evaluate Reverse Polish Notation](https://leetcode.com/problems/evaluate-reverse-polish-notation/) | Medium |
| 5 | [71. Simplify Path](https://leetcode.com/problems/simplify-path/) | Medium |
| 6 | [735. Asteroid Collision](https://leetcode.com/problems/asteroid-collision/) | Medium |
| 7 | [394. Decode String](https://leetcode.com/problems/decode-string/) | Medium |
| 8 | [227. Basic Calculator II](https://leetcode.com/problems/basic-calculator-ii/) | Medium |
| 9 | [224. Basic Calculator](https://leetcode.com/problems/basic-calculator/) | Hard |
| 10 | [32. Longest Valid Parentheses](https://leetcode.com/problems/longest-valid-parentheses/) | Hard |

---

## 4.2 Monotonic Stack

**Signals**
- "**Next / previous greater or smaller** element", "how many days until warmer", "stock span".
- "Largest rectangle", "sum of subarray minimums", "remove k digits to get the smallest number".
- An O(n²) brute force that, for each element, scans left or right for the first element that is bigger or smaller.

**Intuition**
Keep the stack **sorted** (increasing or decreasing). When a new element breaks that order, every element
it pops has just **found its answer**: the new element is their next greater (or smaller).
Each element is pushed once and popped once, so the whole scan is O(n).

| Want | Stack keeps | Pop while |
|------|-------------|-----------|
| Next **greater** | decreasing values | `a[st.top()] < a[i]` |
| Next **smaller** | increasing values | `a[st.top()] > a[i]` |

**Approach**
1. Store **indices** in the stack, not values, so you can compute distances and widths.
2. For each i, pop while the order is broken. Each popped index gets its answer from i.
3. Push i.
4. **Contribution technique** (sum of subarray minimums, histogram): for each element find its previous smaller (`L`) and next smaller (`R`). It is the minimum of `(i-L)*(R-i)` subarrays, and it spans width `R-L-1` in the histogram. With duplicates, make one side non-strict when summing (previous `<`, next `<=`); otherwise a subarray whose minimum appears twice is counted twice.
5. For circular arrays, loop `i` from `0` to `2n-1` and use `i % n`.

**C++ template**
```cpp
vector<int> nextGreater(vector<int>& a) {
    int n = a.size();
    vector<int> res(n, -1);
    stack<int> st;                               // indices, values decreasing
    for (int i = 0; i < n; i++) {
        while (!st.empty() && a[st.top()] < a[i]) {
            res[st.top()] = a[i];
            st.pop();
        }
        st.push(i);
    }
    return res;
}

int largestRectangleArea(vector<int>& h) {
    h.push_back(0);                              // sentinel flushes the stack at the end
    stack<int> st; int best = 0;
    for (int i = 0; i < (int)h.size(); i++) {
        while (!st.empty() && h[st.top()] >= h[i]) {
            int height = h[st.top()]; st.pop();
            int left = st.empty() ? -1 : st.top();
            best = max(best, height * (i - left - 1));
        }
        st.push(i);
    }
    h.pop_back();
    return best;
}

// Greedy monotonic stack: Remove K Digits (smallest number)
string removeKdigits(string num, int k) {
    string st;
    for (char c : num) {
        while (k && !st.empty() && st.back() > c) { st.pop_back(); k--; }
        st.push_back(c);
    }
    st.resize(st.size() - k);
    int i = 0; while (i < (int)st.size() - 1 && st[i] == '0') i++;
    return st.empty() ? "0" : st.substr(i);
}
```

**Complexity:** O(n) time, O(n) space.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [496. Next Greater Element I](https://leetcode.com/problems/next-greater-element-i/) | Easy |
| 2 | [739. Daily Temperatures](https://leetcode.com/problems/daily-temperatures/) | Medium |
| 3 | [503. Next Greater Element II](https://leetcode.com/problems/next-greater-element-ii/) (circular) | Medium |
| 4 | [901. Online Stock Span](https://leetcode.com/problems/online-stock-span/) | Medium |
| 5 | [853. Car Fleet](https://leetcode.com/problems/car-fleet/) | Medium |
| 6 | [402. Remove K Digits](https://leetcode.com/problems/remove-k-digits/) | Medium |
| 7 | [316. Remove Duplicate Letters](https://leetcode.com/problems/remove-duplicate-letters/) | Medium |
| 8 | [907. Sum of Subarray Minimums](https://leetcode.com/problems/sum-of-subarray-minimums/) | Medium |
| 9 | [84. Largest Rectangle in Histogram](https://leetcode.com/problems/largest-rectangle-in-histogram/) | Hard |
| 10 | [85. Maximal Rectangle](https://leetcode.com/problems/maximal-rectangle/) | Hard |

---

## 4.3 Queue & Monotonic Deque

**Signals**
- "Maximum / minimum of **every window** of size k".
- Sliding window where you need the window's max or min in O(1).
- "dp[i] = best of dp[j] over j in [i-k, i-1]", which is a DP optimised with a deque.
- Simulations in arrival order (round-robin, recent calls).

**Intuition**
For a window maximum, an element that is **smaller than a newer element** can never be the maximum again,
because the newer one stays in the window longer and is larger. So drop it from the back.
The deque then holds decreasing values, the front is the current max, and you pop the front once it falls out of the window.

**Approach**
1. Store indices in the deque.
2. Pop the **front** if it's outside the window (`dq.front() <= i - k`).
3. Pop the **back** while `a[dq.back()] <= a[i]` (for a max).
4. Push i. Once `i >= k-1`, the answer is `a[dq.front()]`.

**C++ template**
```cpp
vector<int> maxSlidingWindow(vector<int>& a, int k) {
    deque<int> dq; vector<int> res;
    for (int i = 0; i < (int)a.size(); i++) {
        if (!dq.empty() && dq.front() <= i - k) dq.pop_front();
        while (!dq.empty() && a[dq.back()] <= a[i]) dq.pop_back();
        dq.push_back(i);
        if (i >= k - 1) res.push_back(a[dq.front()]);
    }
    return res;
}

// DP + deque: Jump Game VI, dp[i] = a[i] + max(dp[i-k..i-1])
int maxResult(vector<int>& a, int k) {
    int n = a.size(); vector<int> dp(n); deque<int> dq;
    dp[0] = a[0]; dq.push_back(0);
    for (int i = 1; i < n; i++) {
        if (dq.front() < i - k) dq.pop_front();
        dp[i] = a[i] + dp[dq.front()];
        while (!dq.empty() && dp[dq.back()] <= dp[i]) dq.pop_back();
        dq.push_back(i);
    }
    return dp[n - 1];
}
```

**Complexity:** O(n) time. The deque holds O(k) indices; Jump Game VI also keeps an O(n) `dp` array.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [232. Implement Queue using Stacks](https://leetcode.com/problems/implement-queue-using-stacks/) | Easy |
| 2 | [225. Implement Stack using Queues](https://leetcode.com/problems/implement-stack-using-queues/) | Easy |
| 3 | [933. Number of Recent Calls](https://leetcode.com/problems/number-of-recent-calls/) | Easy |
| 4 | [649. Dota2 Senate](https://leetcode.com/problems/dota2-senate/) | Medium |
| 5 | [950. Reveal Cards In Increasing Order](https://leetcode.com/problems/reveal-cards-in-increasing-order/) | Medium |
| 6 | [1438. Longest Continuous Subarray With Absolute Diff Less Than or Equal to Limit](https://leetcode.com/problems/longest-continuous-subarray-with-absolute-diff-less-than-or-equal-to-limit/) | Medium |
| 7 | [1696. Jump Game VI](https://leetcode.com/problems/jump-game-vi/) | Medium |
| 8 | [239. Sliding Window Maximum](https://leetcode.com/problems/sliding-window-maximum/) | Hard |
| 9 | [1425. Constrained Subsequence Sum](https://leetcode.com/problems/constrained-subsequence-sum/) | Hard |
| 10 | [862. Shortest Subarray with Sum at Least K](https://leetcode.com/problems/shortest-subarray-with-sum-at-least-k/) | Hard |
