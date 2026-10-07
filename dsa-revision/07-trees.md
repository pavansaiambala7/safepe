# 7. Trees

```cpp
struct TreeNode {
    int val; TreeNode *left, *right;
    TreeNode(int x = 0) : val(x), left(nullptr), right(nullptr) {}
};
```

**The main question for any tree problem:**
> "If I already knew the answer for the left and right subtrees, how would I get the answer for this node?"

- If the answer flows **up** from the children → **post-order DFS (bottom-up)**: height, diameter, balanced, max path sum.
- If information flows **down** from the root → **pre-order DFS (top-down)**: pass the path sum, the max so far, or the depth as a parameter.
- If the question is about **levels or the nearest node** → **BFS**.
- If it's a **BST** → use the ordering: inorder traversal is sorted, and each comparison lets you go left or right.

---

## 7.1 DFS: Bottom-up (Post-order)

**Signals**
- "Height / depth", "diameter", "is balanced", "same / symmetric / subtree", "max path sum", "count good nodes".

**Intuition**
Recursion **returns** a value for each subtree. The parent combines its children's results. When the
global answer can pass *through* a node (diameter, max path sum), update a global variable with
`left + right + node`, but **return** only one branch (`node + max(left, right)`), because a path that
continues upward can only use one side.

**Approach**
1. Base case: `if (!root) return 0;` (or a neutral value).
2. `L = dfs(left)`, `R = dfs(right)`.
3. Update the global answer with what passes through this node.
4. Return what the parent needs.

**C++ template**
```cpp
int maxDepth(TreeNode* r) { return r ? 1 + max(maxDepth(r->left), maxDepth(r->right)) : 0; }

int diameter = 0;
int height(TreeNode* r) {
    if (!r) return 0;
    int L = height(r->left), R = height(r->right);
    diameter = max(diameter, L + R);           // path through r
    return 1 + max(L, R);                      // single branch going up
}

int best = INT_MIN;
int maxGain(TreeNode* r) {                     // Binary Tree Maximum Path Sum
    if (!r) return 0;
    int L = max(0, maxGain(r->left)), R = max(0, maxGain(r->right));   // drop negative branches
    best = max(best, r->val + L + R);
    return r->val + max(L, R);
}

// Balanced check: return -1 as an "unbalanced" sentinel
int check(TreeNode* r) {
    if (!r) return 0;
    int L = check(r->left);  if (L < 0) return -1;
    int R = check(r->right); if (R < 0) return -1;
    return abs(L - R) > 1 ? -1 : 1 + max(L, R);
}

// Iterative inorder (useful when recursion depth is a concern)
vector<int> inorder(TreeNode* r) {
    vector<int> res; stack<TreeNode*> st;
    while (r || !st.empty()) {
        while (r) { st.push(r); r = r->left; }
        r = st.top(); st.pop(); res.push_back(r->val); r = r->right;
    }
    return res;
}
```

**Complexity:** O(n) time, O(h) stack space.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [94. Binary Tree Inorder Traversal](https://leetcode.com/problems/binary-tree-inorder-traversal/) | Easy |
| 2 | [104. Maximum Depth of Binary Tree](https://leetcode.com/problems/maximum-depth-of-binary-tree/) | Easy |
| 3 | [226. Invert Binary Tree](https://leetcode.com/problems/invert-binary-tree/) | Easy |
| 4 | [100. Same Tree](https://leetcode.com/problems/same-tree/) | Easy |
| 5 | [101. Symmetric Tree](https://leetcode.com/problems/symmetric-tree/) | Easy |
| 6 | [572. Subtree of Another Tree](https://leetcode.com/problems/subtree-of-another-tree/) | Easy |
| 7 | [543. Diameter of Binary Tree](https://leetcode.com/problems/diameter-of-binary-tree/) | Easy |
| 8 | [110. Balanced Binary Tree](https://leetcode.com/problems/balanced-binary-tree/) | Easy |
| 9 | [1448. Count Good Nodes in Binary Tree](https://leetcode.com/problems/count-good-nodes-in-binary-tree/) | Medium |
| 10 | [124. Binary Tree Maximum Path Sum](https://leetcode.com/problems/binary-tree-maximum-path-sum/) | Hard |

---

## 7.2 BFS: Level Order Traversal

**Signals**
- "Level by level", "zigzag", "right side view", "average / largest per level", "minimum depth", "width", "nodes at distance K".

**Intuition**
A queue visits nodes in order of distance from the root. Taking a **snapshot of the queue size** at the
start of each loop lets you process exactly one level at a time.
For "distance K from a target node", turn the tree into an undirected graph (add parent pointers), then BFS from the target.

**C++ template**
```cpp
vector<vector<int>> levelOrder(TreeNode* root) {
    vector<vector<int>> res;
    if (!root) return res;
    queue<TreeNode*> q; q.push(root);
    while (!q.empty()) {
        int sz = q.size();                       // snapshot = nodes in this level
        vector<int> level;
        for (int i = 0; i < sz; i++) {
            TreeNode* cur = q.front(); q.pop();
            level.push_back(cur->val);
            if (cur->left)  q.push(cur->left);
            if (cur->right) q.push(cur->right);
        }
        res.push_back(level);                    // right view = level.back(); zigzag = reverse odd levels
    }
    return res;
}

// Width: label nodes like a heap (left = 2i, right = 2i+1), subtracting the level's first index to avoid overflow
```

**Complexity:** O(n) time, O(width) space.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [637. Average of Levels in Binary Tree](https://leetcode.com/problems/average-of-levels-in-binary-tree/) | Easy |
| 2 | [111. Minimum Depth of Binary Tree](https://leetcode.com/problems/minimum-depth-of-binary-tree/) | Easy |
| 3 | [102. Binary Tree Level Order Traversal](https://leetcode.com/problems/binary-tree-level-order-traversal/) | Medium |
| 4 | [103. Binary Tree Zigzag Level Order Traversal](https://leetcode.com/problems/binary-tree-zigzag-level-order-traversal/) | Medium |
| 5 | [199. Binary Tree Right Side View](https://leetcode.com/problems/binary-tree-right-side-view/) | Medium |
| 6 | [515. Find Largest Value in Each Tree Row](https://leetcode.com/problems/find-largest-value-in-each-tree-row/) | Medium |
| 7 | [1161. Maximum Level Sum of a Binary Tree](https://leetcode.com/problems/maximum-level-sum-of-a-binary-tree/) | Medium |
| 8 | [116. Populating Next Right Pointers in Each Node](https://leetcode.com/problems/populating-next-right-pointers-in-each-node/) | Medium |
| 9 | [662. Maximum Width of Binary Tree](https://leetcode.com/problems/maximum-width-of-binary-tree/) | Medium |
| 10 | [863. All Nodes Distance K in Binary Tree](https://leetcode.com/problems/all-nodes-distance-k-in-binary-tree/) | Medium |

---

## 7.3 Root-to-Leaf Paths (Top-down) & Lowest Common Ancestor

**Signals**
- "Root-to-leaf path with sum X", "all paths", "sum of numbers formed by paths", "max difference between a node and its ancestor".
- "Lowest common ancestor".

**Intuition**
**Top-down:** pass the accumulated state (path sum, current number, min/max so far) as **parameters**.
Check the answer at a leaf. For "any downward path with sum K" (Path Sum III), use **prefix sums on the path**
with a hash map, the same idea as subarray-sum-equals-K applied to a tree, and undo the map entry when you backtrack.

**LCA:** if p and q are found in different subtrees of a node, that node is the LCA. Otherwise the LCA is
on whichever side returned non-null.

**C++ template**
```cpp
// All root-to-leaf paths with sum == target
vector<vector<int>> res; vector<int> path;
void pathSum(TreeNode* r, int remain) {
    if (!r) return;
    path.push_back(r->val); remain -= r->val;
    if (!r->left && !r->right && remain == 0) res.push_back(path);
    pathSum(r->left, remain); pathSum(r->right, remain);
    path.pop_back();                                   // backtrack
}

// Path Sum III: prefix sums along the current path
unordered_map<long long,int> cnt{{0, 1}};
int countPaths(TreeNode* r, long long sum, int target) {
    if (!r) return 0;
    sum += r->val;
    int res = cnt.count(sum - target) ? cnt[sum - target] : 0;
    cnt[sum]++;
    res += countPaths(r->left, sum, target) + countPaths(r->right, sum, target);
    cnt[sum]--;                                        // undo on the way back
    return res;
}

TreeNode* lca(TreeNode* r, TreeNode* p, TreeNode* q) {
    if (!r || r == p || r == q) return r;
    TreeNode* L = lca(r->left, p, q);
    TreeNode* R = lca(r->right, p, q);
    if (L && R) return r;
    return L ? L : R;
}
```

**Complexity:** O(n) time.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [112. Path Sum](https://leetcode.com/problems/path-sum/) | Easy |
| 2 | [257. Binary Tree Paths](https://leetcode.com/problems/binary-tree-paths/) | Easy |
| 3 | [1022. Sum of Root To Leaf Binary Numbers](https://leetcode.com/problems/sum-of-root-to-leaf-binary-numbers/) | Easy |
| 4 | [113. Path Sum II](https://leetcode.com/problems/path-sum-ii/) | Medium |
| 5 | [129. Sum Root to Leaf Numbers](https://leetcode.com/problems/sum-root-to-leaf-numbers/) | Medium |
| 6 | [437. Path Sum III](https://leetcode.com/problems/path-sum-iii/) | Medium |
| 7 | [1026. Maximum Difference Between Node and Ancestor](https://leetcode.com/problems/maximum-difference-between-node-and-ancestor/) | Medium |
| 8 | [1372. Longest ZigZag Path in a Binary Tree](https://leetcode.com/problems/longest-zigzag-path-in-a-binary-tree/) | Medium |
| 9 | [988. Smallest String Starting From Leaf](https://leetcode.com/problems/smallest-string-starting-from-leaf/) | Medium |
| 10 | [236. Lowest Common Ancestor of a Binary Tree](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree/) | Medium |

---

## 7.4 Binary Search Tree (BST)

**Signals**
- "BST" in the question: search, insert, delete, validate, kth smallest, closest value, iterator.

**Intuition**
- **Inorder traversal of a BST is sorted.** "Kth smallest", "min difference", "recover swapped nodes" and "iterator" all reduce to an inorder walk.
- **Each comparison removes one subtree**, so search, insert and LCA are O(h).
- **Validate** by passing a `(low, high)` range down. Checking only a node against its children isn't enough.

**C++ template**
```cpp
bool isValid(TreeNode* r, long long lo = LLONG_MIN, long long hi = LLONG_MAX) {
    if (!r) return true;
    if (r->val <= lo || r->val >= hi) return false;
    return isValid(r->left, lo, r->val) && isValid(r->right, r->val, hi);
}

int kthSmallest(TreeNode* r, int& k) {               // inorder, stop at k
    if (!r) return -1;
    int L = kthSmallest(r->left, k);
    if (k == 0) return L;
    if (--k == 0) return r->val;
    return kthSmallest(r->right, k);
}

TreeNode* deleteNode(TreeNode* r, int key) {
    if (!r) return r;
    if (key < r->val) r->left = deleteNode(r->left, key);
    else if (key > r->val) r->right = deleteNode(r->right, key);
    else {
        if (!r->left) return r->right;
        if (!r->right) return r->left;
        TreeNode* s = r->right; while (s->left) s = s->left;      // inorder successor
        r->val = s->val;
        r->right = deleteNode(r->right, s->val);
    }
    return r;
}

TreeNode* lcaBST(TreeNode* r, TreeNode* p, TreeNode* q) {
    while (r) {
        if (p->val < r->val && q->val < r->val) r = r->left;
        else if (p->val > r->val && q->val > r->val) r = r->right;
        else return r;
    }
    return nullptr;
}
```

**Complexity:** O(h) per operation (O(log n) when balanced), O(n) for traversals.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [700. Search in a Binary Search Tree](https://leetcode.com/problems/search-in-a-binary-search-tree/) | Easy |
| 2 | [108. Convert Sorted Array to Binary Search Tree](https://leetcode.com/problems/convert-sorted-array-to-binary-search-tree/) | Easy |
| 3 | [530. Minimum Absolute Difference in BST](https://leetcode.com/problems/minimum-absolute-difference-in-bst/) | Easy |
| 4 | [701. Insert into a Binary Search Tree](https://leetcode.com/problems/insert-into-a-binary-search-tree/) | Medium |
| 5 | [450. Delete Node in a BST](https://leetcode.com/problems/delete-node-in-a-bst/) | Medium |
| 6 | [98. Validate Binary Search Tree](https://leetcode.com/problems/validate-binary-search-tree/) | Medium |
| 7 | [230. Kth Smallest Element in a BST](https://leetcode.com/problems/kth-smallest-element-in-a-bst/) | Medium |
| 8 | [235. Lowest Common Ancestor of a Binary Search Tree](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-search-tree/) | Medium |
| 9 | [173. Binary Search Tree Iterator](https://leetcode.com/problems/binary-search-tree-iterator/) | Medium |
| 10 | [99. Recover Binary Search Tree](https://leetcode.com/problems/recover-binary-search-tree/) | Medium |

---

## 7.5 Tree Construction & Serialization

**Signals**
- "Construct a tree from preorder + inorder (or postorder)", "serialize / deserialize", "flatten", "find duplicate subtrees".

**Intuition**
- **Preorder**'s first element is the root. Find it in **inorder**: everything to its left is the left subtree and everything to its right is the right subtree. A hash map from value to inorder index makes the lookup O(1).
- **Serialize** with preorder plus null markers (`#`). That uniquely defines the tree, so deserializing just reads tokens in the same order.
- **Duplicate subtrees:** serialize each subtree into a string key and count the keys in a map.

**C++ template**
```cpp
unordered_map<int,int> inIdx; int preI = 0;
TreeNode* build(vector<int>& pre, int lo, int hi) {           // range of inorder indices
    if (lo > hi) return nullptr;
    TreeNode* root = new TreeNode(pre[preI++]);
    int m = inIdx[root->val];
    root->left  = build(pre, lo, m - 1);
    root->right = build(pre, m + 1, hi);
    return root;
}
// usage: for i, inIdx[in[i]] = i; build(pre, 0, n - 1);

void ser(TreeNode* r, string& out) {                         // append into one string: O(n)
    if (!r) { out += "#,"; return; }
    out += to_string(r->val) + ",";
    ser(r->left, out); ser(r->right, out);
}
string serialize(TreeNode* r) { string s; ser(r, s); return s; }
TreeNode* des(istringstream& in) {
    string tok; getline(in, tok, ',');
    if (tok == "#") return nullptr;
    TreeNode* r = new TreeNode(stoi(tok));
    r->left = des(in); r->right = des(in);
    return r;
}
TreeNode* deserialize(string s) { istringstream in(s); return des(in); }
```

**Complexity:** O(n).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [654. Maximum Binary Tree](https://leetcode.com/problems/maximum-binary-tree/) | Medium |
| 2 | [105. Construct Binary Tree from Preorder and Inorder Traversal](https://leetcode.com/problems/construct-binary-tree-from-preorder-and-inorder-traversal/) | Medium |
| 3 | [106. Construct Binary Tree from Inorder and Postorder Traversal](https://leetcode.com/problems/construct-binary-tree-from-inorder-and-postorder-traversal/) | Medium |
| 4 | [889. Construct Binary Tree from Preorder and Postorder Traversal](https://leetcode.com/problems/construct-binary-tree-from-preorder-and-postorder-traversal/) | Medium |
| 5 | [1008. Construct Binary Search Tree from Preorder Traversal](https://leetcode.com/problems/construct-binary-search-tree-from-preorder-traversal/) | Medium |
| 6 | [114. Flatten Binary Tree to Linked List](https://leetcode.com/problems/flatten-binary-tree-to-linked-list/) | Medium |
| 7 | [1382. Balance a Binary Search Tree](https://leetcode.com/problems/balance-a-binary-search-tree/) | Medium |
| 8 | [652. Find Duplicate Subtrees](https://leetcode.com/problems/find-duplicate-subtrees/) | Medium |
| 9 | [449. Serialize and Deserialize BST](https://leetcode.com/problems/serialize-and-deserialize-bst/) | Medium |
| 10 | [297. Serialize and Deserialize Binary Tree](https://leetcode.com/problems/serialize-and-deserialize-binary-tree/) | Hard |
