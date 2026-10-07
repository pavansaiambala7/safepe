# 9. Graphs

**First, model the problem as a graph.** What are the **nodes** (cells, words, courses, states)? What are the **edges** (neighbouring cells, a one-letter change, a prerequisite)? Is it directed? Weighted?

```cpp
// Adjacency list from an edge list
vector<vector<int>> adj(n);
for (auto& e : edges) { adj[e[0]].push_back(e[1]); adj[e[1]].push_back(e[0]); }  // drop the 2nd push for directed

// Grid neighbours
int dr[] = {-1, 1, 0, 0}, dc[] = {0, 0, -1, 1};
auto inside = [&](int r, int c) { return r >= 0 && c >= 0 && r < R && c < C; };
```

| Question asks for | Use |
|-------------------|-----|
| Connected components / reachability / islands | DFS or BFS (or Union-Find) |
| Shortest path, **unweighted** | BFS |
| Spread from many sources at once | Multi-source BFS |
| Shortest path, weights 0/1 | 0-1 BFS (deque) |
| Shortest path, non-negative weights | Dijkstra |
| Negative weights / at most K edges | Bellman-Ford |
| All-pairs shortest path, n ≤ 400 | Floyd-Warshall |
| Ordering with dependencies / cycle in a directed graph | Topological sort (Kahn) |
| Dynamic connectivity / cycle in an undirected graph | Union-Find |
| Connect everything at minimum cost | MST (Kruskal / Prim) |
| Two-colouring / "split into two groups" | Bipartite check (BFS colouring) |

---

## 9.1 DFS / BFS Traversal & Connected Components

**Signals**
- "Number of islands / provinces / connected groups", "flood fill", "area of the largest island", "can you visit all rooms".
- "Regions surrounded by X", "cells that can reach both oceans". For these, **start from the boundary** and work inward.

**Intuition**
Each DFS or BFS from an unvisited node marks its **whole component**. The number of times you start a new
search = the number of components. **Reverse thinking:** instead of asking "can this cell reach the border?"
for every cell, flood from the border once and mark everything reachable.

**C++ template**
```cpp
void dfs(vector<vector<char>>& g, int r, int c) {
    if (r < 0 || c < 0 || r >= (int)g.size() || c >= (int)g[0].size() || g[r][c] != '1') return;
    g[r][c] = '0';                                        // mark visited
    dfs(g, r + 1, c); dfs(g, r - 1, c); dfs(g, r, c + 1); dfs(g, r, c - 1);
}
int numIslands(vector<vector<char>>& g) {
    int cnt = 0;
    for (int r = 0; r < (int)g.size(); r++)
        for (int c = 0; c < (int)g[0].size(); c++)
            if (g[r][c] == '1') { cnt++; dfs(g, r, c); }
    return cnt;
}

// Generic graph components
vector<bool> vis(n, false);
function<void(int)> go = [&](int u) {
    vis[u] = true;
    for (int v : adj[u]) if (!vis[v]) go(v);
};
int comps = 0;
for (int i = 0; i < n; i++) if (!vis[i]) { comps++; go(i); }
```

**Complexity:** O(V + E), or O(R·C) for a grid.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [733. Flood Fill](https://leetcode.com/problems/flood-fill/) | Easy |
| 2 | [200. Number of Islands](https://leetcode.com/problems/number-of-islands/) | Medium |
| 3 | [695. Max Area of Island](https://leetcode.com/problems/max-area-of-island/) | Medium |
| 4 | [547. Number of Provinces](https://leetcode.com/problems/number-of-provinces/) | Medium |
| 5 | [841. Keys and Rooms](https://leetcode.com/problems/keys-and-rooms/) | Medium |
| 6 | [133. Clone Graph](https://leetcode.com/problems/clone-graph/) | Medium |
| 7 | [130. Surrounded Regions](https://leetcode.com/problems/surrounded-regions/) | Medium |
| 8 | [1020. Number of Enclaves](https://leetcode.com/problems/number-of-enclaves/) | Medium |
| 9 | [1254. Number of Closed Islands](https://leetcode.com/problems/number-of-closed-islands/) | Medium |
| 10 | [417. Pacific Atlantic Water Flow](https://leetcode.com/problems/pacific-atlantic-water-flow/) | Medium |

---

## 9.2 BFS Shortest Path (Unweighted) & Multi-source BFS

**Signals**
- "**Minimum steps / moves / time**" where every move costs the same.
- "Rotting oranges", "distance to the nearest 0", "word ladder", "open the lock" (the states are the nodes).
- Several starting points spreading at the same time → **multi-source BFS**.

**Intuition**
BFS explores in **rings of increasing distance**, so the first time you reach a node is by a shortest path.
**Multi-source:** push *all* sources into the queue at distance 0. It works as if a virtual super-source connected to all of them.
If the state includes extra information (keys collected, obstacles removed), make it part of the node: `(r, c, k)`.

**C++ template**
```cpp
int bfsGrid(vector<vector<int>>& g, vector<pii> sources) {
    int R = g.size(), C = g[0].size();
    vector<vector<int>> dist(R, vector<int>(C, -1));
    queue<pii> q;
    for (auto [r, c] : sources) { dist[r][c] = 0; q.push({r, c}); }
    int dr[] = {-1, 1, 0, 0}, dc[] = {0, 0, -1, 1}, maxD = 0;
    while (!q.empty()) {
        auto [r, c] = q.front(); q.pop();
        for (int d = 0; d < 4; d++) {
            int nr = r + dr[d], nc = c + dc[d];
            if (nr < 0 || nc < 0 || nr >= R || nc >= C) continue;
            if (dist[nr][nc] != -1 || g[nr][nc] == 0) continue;   // visited or blocked
            dist[nr][nc] = dist[r][c] + 1;                         // mark when PUSHING, not popping
            maxD = max(maxD, dist[nr][nc]);
            q.push({nr, nc});
        }
    }
    return maxD;
}

// Word Ladder: implicit graph; neighbours = change one letter
int ladderLength(string begin, string end, vector<string>& words) {
    unordered_set<string> dict(words.begin(), words.end());
    if (!dict.count(end)) return 0;
    queue<string> q; q.push(begin); dict.erase(begin);
    for (int steps = 1; !q.empty(); steps++) {
        for (int sz = q.size(); sz--; ) {
            string w = q.front(); q.pop();
            if (w == end) return steps;
            for (char& ch : w) {
                char orig = ch;
                for (char x = 'a'; x <= 'z'; x++) {
                    ch = x;
                    if (dict.count(w)) { q.push(w); dict.erase(w); }
                }
                ch = orig;
            }
        }
    }
    return 0;
}
```

**Complexity:** O(V + E).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [994. Rotting Oranges](https://leetcode.com/problems/rotting-oranges/) | Medium |
| 2 | [542. 01 Matrix](https://leetcode.com/problems/01-matrix/) | Medium |
| 3 | [1091. Shortest Path in Binary Matrix](https://leetcode.com/problems/shortest-path-in-binary-matrix/) | Medium |
| 4 | [1926. Nearest Exit from Entrance in Maze](https://leetcode.com/problems/nearest-exit-from-entrance-in-maze/) | Medium |
| 5 | [1162. As Far from Land as Possible](https://leetcode.com/problems/as-far-from-land-as-possible/) | Medium |
| 6 | [752. Open the Lock](https://leetcode.com/problems/open-the-lock/) | Medium |
| 7 | [433. Minimum Genetic Mutation](https://leetcode.com/problems/minimum-genetic-mutation/) | Medium |
| 8 | [934. Shortest Bridge](https://leetcode.com/problems/shortest-bridge/) | Medium |
| 9 | [127. Word Ladder](https://leetcode.com/problems/word-ladder/) | Hard |
| 10 | [1293. Shortest Path in a Grid with Obstacles Elimination](https://leetcode.com/problems/shortest-path-in-a-grid-with-obstacles-elimination/) | Hard |

---

## 9.3 Topological Sort

**Signals**
- "Prerequisites", "course schedule", "build order", "recipes from ingredients", "can all tasks finish?".
- A **directed** graph where you need an ordering, or need to detect a cycle.
- "Longest path in a DAG" or "minimum time with parallel tasks" (DP over the topological order).

**Intuition (Kahn's algorithm)**
A node with **indegree 0** has no unmet dependencies, so it can go first. Remove it, which lowers its
neighbours' indegrees, and repeat. If you process fewer than n nodes, the graph has a **cycle** (every leftover node is on a cycle or reachable from one).
DFS alternative: append a node *after* visiting all its descendants, then reverse. Use 3 colours (white, grey, black) to detect cycles.

**C++ template**
```cpp
vector<int> topoSort(int n, vector<vector<int>>& edges) {     // edge u -> v
    vector<vector<int>> adj(n); vector<int> indeg(n, 0);
    for (auto& e : edges) { adj[e[0]].push_back(e[1]); indeg[e[1]]++; }
    queue<int> q;
    for (int i = 0; i < n; i++) if (indeg[i] == 0) q.push(i);
    vector<int> order;
    while (!q.empty()) {
        int u = q.front(); q.pop(); order.push_back(u);
        for (int v : adj[u]) if (--indeg[v] == 0) q.push(v);
    }
    if ((int)order.size() < n) return {};                      // cycle
    return order;
}
// Course Schedule: prerequisites [a, b] means the edge is b -> a.
// Parallel Courses III: finish[v] = max(finish[v], finish[u] + time[v]) while processing u -> v.
```

**Complexity:** O(V + E).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [207. Course Schedule](https://leetcode.com/problems/course-schedule/) | Medium |
| 2 | [210. Course Schedule II](https://leetcode.com/problems/course-schedule-ii/) | Medium |
| 3 | [802. Find Eventual Safe States](https://leetcode.com/problems/find-eventual-safe-states/) | Medium |
| 4 | [1462. Course Schedule IV](https://leetcode.com/problems/course-schedule-iv/) | Medium |
| 5 | [2115. Find All Possible Recipes from Given Supplies](https://leetcode.com/problems/find-all-possible-recipes-from-given-supplies/) | Medium |
| 6 | [310. Minimum Height Trees](https://leetcode.com/problems/minimum-height-trees/) (peel leaves layer by layer) | Medium |
| 7 | [2050. Parallel Courses III](https://leetcode.com/problems/parallel-courses-iii/) | Hard |
| 8 | [329. Longest Increasing Path in a Matrix](https://leetcode.com/problems/longest-increasing-path-in-a-matrix/) | Hard |
| 9 | [2392. Build a Matrix With Conditions](https://leetcode.com/problems/build-a-matrix-with-conditions/) | Hard |
| 10 | [1203. Sort Items by Groups Respecting Dependencies](https://leetcode.com/problems/sort-items-by-groups-respecting-dependencies/) | Hard |

---

## 9.4 Union-Find (Disjoint Set Union)

**Signals**
- "Are x and y connected?" with edges **added over time**.
- "Redundant connection" (the edge that creates a cycle), "accounts merge", "equations a==b, a!=c", "number of groups after merges".
- Kruskal's MST.

**Intuition**
Each component is a tree, identified by its **root**. `find(x)` walks up to the root; **path compression**
points every visited node directly at the root. `unite(a, b)` attaches one root under the other, choosing
by **size or rank** to keep the trees shallow. Together these make each operation almost O(1) (α(n)).

**C++ template**
```cpp
struct DSU {
    vector<int> p, sz; int comps;
    DSU(int n) : p(n), sz(n, 1), comps(n) { iota(p.begin(), p.end(), 0); }
    int find(int x) { return p[x] == x ? x : p[x] = find(p[x]); }
    bool unite(int a, int b) {
        a = find(a); b = find(b);
        if (a == b) return false;                  // already connected, so this edge forms a cycle
        if (sz[a] < sz[b]) swap(a, b);
        p[b] = a; sz[a] += sz[b]; comps--;
        return true;
    }
};

vector<int> findRedundantConnection(vector<vector<int>>& edges) {
    DSU d(edges.size() + 1);
    for (auto& e : edges) if (!d.unite(e[0], e[1])) return e;
    return {};
}
// Grid cell (r, c) -> id r * C + c
```

**Complexity:** O(α(n)) per operation, which is effectively constant.

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [1971. Find if Path Exists in Graph](https://leetcode.com/problems/find-if-path-exists-in-graph/) | Easy |
| 2 | [684. Redundant Connection](https://leetcode.com/problems/redundant-connection/) | Medium |
| 3 | [1319. Number of Operations to Make Network Connected](https://leetcode.com/problems/number-of-operations-to-make-network-connected/) | Medium |
| 4 | [990. Satisfiability of Equality Equations](https://leetcode.com/problems/satisfiability-of-equality-equations/) | Medium |
| 5 | [2316. Count Unreachable Pairs of Nodes in an Undirected Graph](https://leetcode.com/problems/count-unreachable-pairs-of-nodes-in-an-undirected-graph/) | Medium |
| 6 | [721. Accounts Merge](https://leetcode.com/problems/accounts-merge/) | Medium |
| 7 | [947. Most Stones Removed with Same Row or Column](https://leetcode.com/problems/most-stones-removed-with-same-row-or-column/) | Medium |
| 8 | [1202. Smallest String With Swaps](https://leetcode.com/problems/smallest-string-with-swaps/) | Medium |
| 9 | [839. Similar String Groups](https://leetcode.com/problems/similar-string-groups/) | Hard |
| 10 | [1579. Remove Max Number of Edges to Keep Graph Fully Traversable](https://leetcode.com/problems/remove-max-number-of-edges-to-keep-graph-fully-traversable/) | Hard |

---

## 9.5 Weighted Shortest Path (Dijkstra, Bellman-Ford, 0-1 BFS, Floyd)

**Signals**
- "Minimum cost / time / effort" with **different edge weights**.
- "Within K stops" → Bellman-Ford with K+1 rounds, or BFS over the state `(node, stops)`.
- "Maximum probability" → Dijkstra with a max-heap on the product.
- "Minimise the maximum edge on the path" (path with minimum effort) → Dijkstra where the path cost is `max(edge)` instead of the sum.
- Only weights 0 and 1 → 0-1 BFS.

**Intuition**
**Dijkstra** is greedy: the unvisited node with the smallest tentative distance is final, because with
non-negative edges no other path can come back cheaper. A min-heap picks that node each time.
**Lazy deletion:** skip a popped entry if `d > dist[u]` (it's an outdated copy).

**C++ template**
```cpp
vector<ll> dijkstra(int n, vector<vector<pii>>& adj, int src) {   // adj[u] = {v, w}
    vector<ll> dist(n, LLONG_MAX);
    priority_queue<pair<ll,int>, vector<pair<ll,int>>, greater<>> pq;
    dist[src] = 0; pq.push({0, src});
    while (!pq.empty()) {
        auto [d, u] = pq.top(); pq.pop();
        if (d > dist[u]) continue;                                  // outdated entry
        for (auto [v, w] : adj[u])
            if (dist[u] + w < dist[v]) { dist[v] = dist[u] + w; pq.push({dist[v], v}); }
    }
    return dist;
}

// Bellman-Ford limited to K edges (Cheapest Flights Within K Stops)
int cheapest(int n, vector<vector<int>>& flights, int src, int dst, int k) {
    vector<int> dist(n, INT_MAX); dist[src] = 0;
    for (int i = 0; i <= k; i++) {
        vector<int> nd = dist;                                      // copy so each round adds at most one edge
        for (auto& f : flights)
            if (dist[f[0]] != INT_MAX) nd[f[1]] = min(nd[f[1]], dist[f[0]] + f[2]);
        dist = nd;
    }
    return dist[dst] == INT_MAX ? -1 : dist[dst];
}

// 0-1 BFS: weight-0 edges go to the front of the deque, weight-1 edges to the back
// Floyd-Warshall: for k, for i, for j: d[i][j] = min(d[i][j], d[i][k] + d[k][j])
```

**Complexity:** Dijkstra O((V+E) log V), Bellman-Ford O(K·E), Floyd O(V³).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [743. Network Delay Time](https://leetcode.com/problems/network-delay-time/) | Medium |
| 2 | [1514. Path with Maximum Probability](https://leetcode.com/problems/path-with-maximum-probability/) | Medium |
| 3 | [1631. Path With Minimum Effort](https://leetcode.com/problems/path-with-minimum-effort/) | Medium |
| 4 | [787. Cheapest Flights Within K Stops](https://leetcode.com/problems/cheapest-flights-within-k-stops/) | Medium |
| 5 | [1334. Find the City With the Smallest Number of Neighbors at a Threshold Distance](https://leetcode.com/problems/find-the-city-with-the-smallest-number-of-neighbors-at-a-threshold-distance/) (Floyd) | Medium |
| 6 | [1976. Number of Ways to Arrive at Destination](https://leetcode.com/problems/number-of-ways-to-arrive-at-destination/) | Medium |
| 7 | [778. Swim in Rising Water](https://leetcode.com/problems/swim-in-rising-water/) | Hard |
| 8 | [2290. Minimum Obstacle Removal to Reach Corner](https://leetcode.com/problems/minimum-obstacle-removal-to-reach-corner/) (0-1 BFS) | Hard |
| 9 | [1368. Minimum Cost to Make at Least One Valid Path in a Grid](https://leetcode.com/problems/minimum-cost-to-make-at-least-one-valid-path-in-a-grid/) | Hard |
| 10 | [882. Reachable Nodes In Subdivided Graph](https://leetcode.com/problems/reachable-nodes-in-subdivided-graph/) | Hard |

---

## 9.6 MST, Bipartite & Advanced Graph Patterns

**Signals**
- "Connect all points / cities at minimum total cost" → **MST**.
- "Split into two groups with no conflicts inside a group", "is the graph bipartite" → **2-colouring**.
- "Critical connections / bridges" → **Tarjan's low-link**.
- "Use every edge exactly once" (itinerary) → **Eulerian path (Hierholzer)**.
- Functional graphs (each node has one outgoing edge) → cycle detection with timestamps.

**Intuition**
- **Kruskal:** sort edges by weight and add each edge that connects two different components (Union-Find). Cut property: the cheapest edge crossing any cut is always safe to take.
- **Prim:** grow a tree from one node, always adding the cheapest edge that leaves the tree (min-heap). Better for dense graphs, like all pairs of points.
- **Bipartite:** BFS and colour neighbours with the opposite colour. Two neighbours with the same colour means it isn't bipartite (there's an odd cycle).
- **Bridges:** `low[v]` = the smallest discovery time reachable from v's subtree using one back edge. Edge `(u, v)` is a bridge iff `low[v] > disc[u]`.

**C++ template**
```cpp
// Kruskal
int kruskal(int n, vector<array<int,3>>& edges) {          // {w, u, v}
    sort(edges.begin(), edges.end());
    DSU d(n); int cost = 0, used = 0;
    for (auto& [w, u, v] : edges)
        if (d.unite(u, v)) { cost += w; if (++used == n - 1) break; }
    return cost;
}

// Bipartite check
bool isBipartite(vector<vector<int>>& g) {
    int n = g.size(); vector<int> color(n, -1);
    for (int s = 0; s < n; s++) {
        if (color[s] != -1) continue;
        queue<int> q; q.push(s); color[s] = 0;
        while (!q.empty()) {
            int u = q.front(); q.pop();
            for (int v : g[u]) {
                if (color[v] == -1) { color[v] = color[u] ^ 1; q.push(v); }
                else if (color[v] == color[u]) return false;
            }
        }
    }
    return true;
}

// Tarjan bridges
vector<int> disc, low; vector<vector<int>> bridges; int timer_ = 0;
void tarjan(int u, int parent, vector<vector<int>>& adj) {
    disc[u] = low[u] = timer_++;
    for (int v : adj[u]) {
        if (v == parent) continue;
        if (disc[v] == -1) {
            tarjan(v, u, adj);
            low[u] = min(low[u], low[v]);
            if (low[v] > disc[u]) bridges.push_back({u, v});
        } else low[u] = min(low[u], disc[v]);
    }
}
```

**Complexity:** Kruskal O(E log E), Prim O(E log V), bipartite / Tarjan O(V + E).

**Practice (10)**
| # | Problem | Level |
|---|---------|-------|
| 1 | [997. Find the Town Judge](https://leetcode.com/problems/find-the-town-judge/) (indegree / outdegree) | Easy |
| 2 | [1557. Minimum Number of Vertices to Reach All Nodes](https://leetcode.com/problems/minimum-number-of-vertices-to-reach-all-nodes/) | Medium |
| 3 | [785. Is Graph Bipartite?](https://leetcode.com/problems/is-graph-bipartite/) | Medium |
| 4 | [886. Possible Bipartition](https://leetcode.com/problems/possible-bipartition/) | Medium |
| 5 | [1584. Min Cost to Connect All Points](https://leetcode.com/problems/min-cost-to-connect-all-points/) | Medium |
| 6 | [2101. Detonate the Maximum Bombs](https://leetcode.com/problems/detonate-the-maximum-bombs/) | Medium |
| 7 | [2360. Longest Cycle in a Graph](https://leetcode.com/problems/longest-cycle-in-a-graph/) | Hard |
| 8 | [1192. Critical Connections in a Network](https://leetcode.com/problems/critical-connections-in-a-network/) | Hard |
| 9 | [332. Reconstruct Itinerary](https://leetcode.com/problems/reconstruct-itinerary/) | Hard |
| 10 | [1489. Find Critical and Pseudo-Critical Edges in Minimum Spanning Tree](https://leetcode.com/problems/find-critical-and-pseudo-critical-edges-in-minimum-spanning-tree/) | Hard |
