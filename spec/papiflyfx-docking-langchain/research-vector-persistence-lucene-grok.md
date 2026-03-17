**Yes, Apache Lucene can serve as a vector persistence storage** — and in practice it already does so very effectively in 2025–2026, especially for applications that need **hybrid lexical + semantic search**, good memory efficiency, disk-based persistence, and integration with full-text features.

### Key Points (Current Status)
- **Since ~2022–2023** Lucene natively supports **dense vector fields** (KNN vectors) with approximate nearest-neighbor (ANN) search using **HNSW** graphs.
- It stores dense vectors (float or byte quantized) on disk in immutable segments → classic Lucene design: segments are merged over time, vectors live mostly on disk but benefit from OS page cache.
- Supported similarities: **cosine**, **dot product** (inner product), **Euclidean** (L2).
- Max dimensions increased over time (from 1024 → higher values like 2048+ in recent versions).
- **Vector persistence** is built-in: vectors are serialized into Lucene's segment files (`.vec`, `.vd`, etc. depending on codec).
- **No need for external blob storage** — everything (text, metadata, vectors) lives in the same index.

### Comparison: Lucene vs "pure" vector databases

| Aspect                        | Pure vector DBs (Pinecone, Weaviate, Milvus, Qdrant, etc.) | Apache Lucene (standalone or via Solr/Elasticsearch/OpenSearch) |
|-------------------------------|-------------------------------------------------------------|-----------------------------------------------------------------|
| Primary focus                 | Dense vector ANN search + metadata filtering                | Full-text + dense vector + hybrid + faceting + sorting          |
| Persistence                   | Usually memory-first or custom on-disk                      | Disk-first (immutable segments + merge policy)                  |
| Memory efficiency             | Often loads full index into RAM                             | Excellent — partial loading, page-cache friendly                |
| Update/delete cost            | Usually cheap (mutable indexes)                             | More expensive (delete-by-query or soft-deletes + merges)       |
| Hybrid lexical + vector       | Often added later (sometimes weak BM25)                     | Native & very strong (BM25 + rescoring + filters)               |
| Filtered vector search perf   | Varies (some excellent)                                     | Very good (especially recent improvements: ACORN, etc.)         |
| Quantization / compression    | Usually yes                                                 | Yes (scalar quantization, Lucene 9+)                            |
| Max scale (single node)       | Often very high                                             | High, but shines more in distributed setups                     |
| Ecosystem maturity            | 3–7 years                                                   | 25+ years                                                       |

### When Lucene is an excellent choice for vector persistence
- You already use (or want) **full-text search**, **BM25**, **facets**, **highlighting**, **grouping**, **geospatial**, etc.
- You need **hybrid search** (keyword + semantic) without stitching two systems together.
- Memory footprint matters (Lucene is famously stingy compared to pure in-memory vector DBs).
- You're building on **Java/Solr/Elasticsearch/OpenSearch** anyway.
- Long-term stability and battle-tested storage format is important.

### When you might prefer a dedicated vector store instead
- Pure billion-scale vector-only workloads with very frequent small updates.
- Need sub-millisecond latencies at extreme QPS with simple metadata filters.
- Heavy use of advanced clustering/partitioning techniques not in Lucene.
- You want the absolute simplest vector-only API.

### Bottom line (2026 perspective)
Many teams have concluded **"Lucene is all you need"** for a very large class of real-world semantic/hybrid search applications — especially after the massive vector performance & efficiency investments in Lucene 9.x → 10.x releases (2023–2025).

Elastic themselves explicitly call their goal: **"make Apache Lucene the best vector database"**.

So yes — Lucene does act as production-grade **vector persistence storage** today, and for many use-cases it's not just "possible", it's actually one of the strongest options available.