# 5. Linked List

Linked list problems are mostly about **pointer bookkeeping**. Three tools cover nearly all of them:
**reversal**, **fast & slow pointers**, and a **dummy node**. Draw boxes and arrows on paper, because
most bugs come from losing a `next` pointer.

```cpp
struct ListNode {
    int val; ListNode* next;
    ListNode(int x = 0, ListNode* n = nullptr) : val(x), next(n) {}
};
```

---

## 5.1 In-place Reversal

**Signals**
- "Reverse the list / a sublist `[left, right]` / every k nodes".
- "Palindrome list", "reorder list (L0 → Ln → L1 → Ln-1 ...)", "add numbers stored in forward order".

**Intuition**
Reversing only needs three pointers: `prev`, `cur` and `next`. At each step point `cur->next` back to
`prev`, then move all three forward. For a sublist, reverse that part and **reconnect** both ends.
Many medium problems combine three steps: *find the middle, reverse the second half, then merge or compare*.

**Approach**
1. Save `nxt = cur->next`, set `cur->next = prev`, then `prev = cur` and `cur = nxt`.
2. Sublist: walk to the node *before* `left` (use a dummy node), reverse `right-left+1` nodes, reconnect.
3. k-group: check that k nodes exist first, reverse them, then recurse or loop on the rest.

**C++ template**
```cpp
ListNode* reverseList(ListNode* head) {
    ListNode *prev = nullptr, *cur = head;
    while (cur) {
        ListNode* nxt = cur->next;
        cur->next = prev;
        prev = cur; cur = nxt;
    }
    return prev;
}

ListNode* reverseBetween(ListNode* head, int left, int right) {
    ListNode dummy(0, head), *pre = &dummy;
    for (int i = 1; i < left; i++) pre = pre->next;
    ListNode* cur = pre->next;
    for (int i = 0; i < right - left; i++) {       // move cur->next to the front of the sublist
        ListNode* nxt = cur->next;
        cur->next = nxt->next;
        nxt->next = pre->next;
        pre->next = nxt;
    }
    return dummy.next;
}

ListNode* reverseKGroup(ListNode* head, int k) {
    ListNode* node = head;
    for (int i = 0; i < k; i++) { if (!node) return head; node = node->next; }
    ListNode *prev = reverseKGroup(node, k), *cur = head;
    for (int i = 0; i < k; i++) {
        ListNode* nxt = cur->next; cur->next = prev; prev = cur; cur = nxt;
    }
    return prev;
}
```

**Complexity:** O(n) time, O(1) space for the iterative version.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [206. Reverse Linked List](https://leetcode.com/problems/reverse-linked-list/) | Easy |
| 2 | [234. Palindrome Linked List](https://leetcode.com/problems/palindrome-linked-list/) | Easy |
| 3 | [92. Reverse Linked List II](https://leetcode.com/problems/reverse-linked-list-ii/) | Medium |
| 4 | [24. Swap Nodes in Pairs](https://leetcode.com/problems/swap-nodes-in-pairs/) | Medium |
| 5 | [61. Rotate List](https://leetcode.com/problems/rotate-list/) | Medium |
| 6 | [143. Reorder List](https://leetcode.com/problems/reorder-list/) | Medium |
| 7 | [2130. Maximum Twin Sum of a Linked List](https://leetcode.com/problems/maximum-twin-sum-of-a-linked-list/) | Medium |
| 8 | [445. Add Two Numbers II](https://leetcode.com/problems/add-two-numbers-ii/) | Medium |
| 9 | [2074. Reverse Nodes in Even Length Groups](https://leetcode.com/problems/reverse-nodes-in-even-length-groups/) | Medium |
| 10 | [25. Reverse Nodes in k-Group](https://leetcode.com/problems/reverse-nodes-in-k-group/) | Hard |

---

## 5.2 Fast & Slow Pointers (Floyd)

**Signals**
- "Detect a cycle", "find where the cycle starts", "middle of the list", "nth node from the end".
- Any sequence `x → f(x)` that might loop (Happy Number, Find the Duplicate Number).

**Intuition**
- **Middle:** fast moves 2 steps for each step slow takes, so when fast reaches the end, slow is at the middle.
- **Cycle:** inside a loop, fast gains one step on slow each move, so they must meet.
- **Cycle start:** after they meet, move one pointer back to the head and step both one at a time. They meet at the cycle's entrance. (If the distance to the entrance is `a`, then `a ≡ distance from the meeting point to the entrance (mod cycle length)`.)
- **Nth from the end:** move fast n steps ahead, then move both. When fast reaches the end, slow is n from the end.

**C++ template**
```cpp
ListNode* middle(ListNode* head) {               // second middle for even lengths
    ListNode *s = head, *f = head;
    while (f && f->next) { s = s->next; f = f->next->next; }
    return s;
}

ListNode* detectCycle(ListNode* head) {
    ListNode *s = head, *f = head;
    while (f && f->next) {
        s = s->next; f = f->next->next;
        if (s == f) {
            s = head;
            while (s != f) { s = s->next; f = f->next; }
            return s;
        }
    }
    return nullptr;
}

ListNode* removeNthFromEnd(ListNode* head, int n) {
    ListNode dummy(0, head), *s = &dummy, *f = &dummy;
    for (int i = 0; i <= n; i++) f = f->next;
    while (f) { s = s->next; f = f->next; }
    s->next = s->next->next;
    return dummy.next;
}

// Find the Duplicate Number: treat i -> nums[i] as a linked list with a cycle
int findDuplicate(vector<int>& a) {
    int s = a[0], f = a[0];
    do { s = a[s]; f = a[a[f]]; } while (s != f);
    s = a[0];
    while (s != f) { s = a[s]; f = a[f]; }
    return s;
}
```

**Complexity:** O(n) time, O(1) space.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [141. Linked List Cycle](https://leetcode.com/problems/linked-list-cycle/) | Easy |
| 2 | [876. Middle of the Linked List](https://leetcode.com/problems/middle-of-the-linked-list/) | Easy |
| 3 | [202. Happy Number](https://leetcode.com/problems/happy-number/) | Easy |
| 4 | [160. Intersection of Two Linked Lists](https://leetcode.com/problems/intersection-of-two-linked-lists/) | Easy |
| 5 | [142. Linked List Cycle II](https://leetcode.com/problems/linked-list-cycle-ii/) | Medium |
| 6 | [19. Remove Nth Node From End of List](https://leetcode.com/problems/remove-nth-node-from-end-of-list/) | Medium |
| 7 | [2095. Delete the Middle Node of a Linked List](https://leetcode.com/problems/delete-the-middle-node-of-a-linked-list/) | Medium |
| 8 | [1721. Swapping Nodes in a Linked List](https://leetcode.com/problems/swapping-nodes-in-a-linked-list/) | Medium |
| 9 | [287. Find the Duplicate Number](https://leetcode.com/problems/find-the-duplicate-number/) | Medium |
| 10 | [457. Circular Array Loop](https://leetcode.com/problems/circular-array-loop/) | Medium |

---

## 5.3 Dummy Node, Merging & Design

**Signals**
- "Merge two sorted lists", "add two numbers", "remove all nodes with value X", "partition around X".
- The head itself might change or be deleted.
- Design problems: LRU cache, copying a list with random pointers.

**Intuition**
A **dummy (sentinel) node** placed before the head means the head is no longer a special case: every real
node has a previous node. Build the result by appending to a `tail` pointer.
**LRU Cache** = hash map (key → list node) + doubly linked list (most recent at the front), which makes both get and put O(1).

**C++ template**
```cpp
ListNode* mergeTwoLists(ListNode* a, ListNode* b) {
    ListNode dummy, *tail = &dummy;
    while (a && b) {
        if (a->val <= b->val) { tail->next = a; a = a->next; }
        else                  { tail->next = b; b = b->next; }
        tail = tail->next;
    }
    tail->next = a ? a : b;
    return dummy.next;
}

ListNode* addTwoNumbers(ListNode* a, ListNode* b) {
    ListNode dummy, *tail = &dummy; int carry = 0;
    while (a || b || carry) {
        int s = carry + (a ? a->val : 0) + (b ? b->val : 0);
        carry = s / 10;
        tail->next = new ListNode(s % 10); tail = tail->next;
        if (a) a = a->next; if (b) b = b->next;
    }
    return dummy.next;
}

class LRUCache {
    int cap;
    list<pair<int,int>> dll;                                   // front = most recently used
    unordered_map<int, list<pair<int,int>>::iterator> pos;
public:
    LRUCache(int c) : cap(c) {}
    int get(int k) {
        if (!pos.count(k)) return -1;
        dll.splice(dll.begin(), dll, pos[k]);                  // move to front in O(1)
        return pos[k]->second;
    }
    void put(int k, int v) {
        if (pos.count(k)) { pos[k]->second = v; dll.splice(dll.begin(), dll, pos[k]); return; }
        if ((int)dll.size() == cap) { pos.erase(dll.back().first); dll.pop_back(); }
        dll.push_front({k, v}); pos[k] = dll.begin();
    }
};
```

**Complexity:** O(n) for list operations, O(1) for each LRU operation.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [21. Merge Two Sorted Lists](https://leetcode.com/problems/merge-two-sorted-lists/) | Easy |
| 2 | [83. Remove Duplicates from Sorted List](https://leetcode.com/problems/remove-duplicates-from-sorted-list/) | Easy |
| 3 | [203. Remove Linked List Elements](https://leetcode.com/problems/remove-linked-list-elements/) | Easy |
| 4 | [2. Add Two Numbers](https://leetcode.com/problems/add-two-numbers/) | Medium |
| 5 | [82. Remove Duplicates from Sorted List II](https://leetcode.com/problems/remove-duplicates-from-sorted-list-ii/) | Medium |
| 6 | [86. Partition List](https://leetcode.com/problems/partition-list/) | Medium |
| 7 | [328. Odd Even Linked List](https://leetcode.com/problems/odd-even-linked-list/) | Medium |
| 8 | [148. Sort List](https://leetcode.com/problems/sort-list/) (merge sort on a list) | Medium |
| 9 | [138. Copy List with Random Pointer](https://leetcode.com/problems/copy-list-with-random-pointer/) | Medium |
| 10 | [146. LRU Cache](https://leetcode.com/problems/lru-cache/) | Medium |
