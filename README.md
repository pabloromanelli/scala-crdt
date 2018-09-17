# Scala CRDTs

### Features
- Immutable
- Property tested
- Uses Dotted Version Vector Sets to ease causal CRDTs development
- Delta Replication Enabled
- Configurable serialization / Multi-lang interoperability
- Configurable persistent backend
- Extensible: Create your own type of CRDT

## Causality Tracking

### Dot Context
Provides a compact causal history.

### Dot Kernel
Provides values with their related causal information.

#### DotKernel Merge Diagram

```
this Kernel
 ___________________________
|                           |
|  this context             |
|     ________________      |
|     |+this +++++++++|     |
|     |+dotted vals ++|     |
|     |++++ __________|_____|__________ 
|     |++++|----------|     |          |
|     |++++|----------|     |          |
|     |++++|-----_____|_____|____      |
|     |++++|-----|++++|-----|++++|     |
|     |++++|-----|++++|-----|++++|     |
|     |++++|-----|++++|-----|++++|     |
|     |++++|-----|++++|-----|++++|     |
|     |++++|-----|++++|-----|++++|     |
|     |____|_____|____|-----|++++|     |
|          |     |----------|++++|     |
|          |     |----------|++++|     |
|__________|_____|__________|++++|     |
           |     |+ other +++++++|     |
           |     |+ dotted vals +|     |
           |     |_______________|     |
           |                           |
           |   other context           |
           |___________________________|
                          other Kernel
```

### TODO
- Use server ids instead of client ids to improve scalability (instead of client ids when creating the value)
  - Causality update coalescing (only set an id when querying the server)
- Investigate causal transactions (to allow client to see a group of updates on multiple CRDTs or none of them)

### References
- Lamport, L. (1978). "Time, clocks, and the ordering of events in a distributed system" (PDF). Communications of the ACM . 21 (7): 558–565. doi:10.1145/359545.359563.
- Mattern, F. (October 1988), "Virtual Time and Global States of Distributed Systems", in Cosnard, M., Proc. Workshop on Parallel and Distributed Algorithms, Chateau de Bonas, France: Elsevier, pp. 215–226
- Dynamo: Amazon’s Highly Available Key-value Store, SOSP 2007
- Shapiro, Marc; Preguiça, Nuno; Baquero, Carlos; Zawirski, Marek (2011), Conflict-Free Replicated Data Types, Lecture Notes in Computer Science, 6976 (Proc 13th International Symposium, SSS 2011), Grenoble, France: Springer Berlin Heidelberg, pp. 386–400, ISBN 978-3-642-24549-7, doi:10.1007/978-3-642-24550-3_29
- Shapiro, Marc; Preguiça, Nuno; Baquero, Carlos; Zawirski, Marek (13 January 2011). "A Comprehensive Study of Convergent and Commutative Replicated Data Types". RR-7506. HAL - Inria.
- Shapiro, Marc; Preguiça, Nuno (2007). "Designing a Commutative Replicated Data Type". Computing Research Repository (CoRR). abs/0710.1784.
- Letia, Mihai; Preguiça, Nuno; Shapiro, Marc (2009). "CRDTs: Consistency without Concurrency Control". Computing Research Repository (CoRR). abs/0907.0929.
- Letia, Mihai; Preguiça, Nuno; Shapiro, Marc (1 April 2010). "Consistency without Concurrency Control in Large, Dynamic Systems". SIGOPS Oper. Syst. Rev. ACM. 44: 29–34. doi:10.1145/1773912.1773921.
- Baquero, Carlos; Almeida, Paulo Sérgio; Shoker, Ali (2014-06-03). Magoutis, Kostas; Pietzuch, Peter, eds. Making Operation-Based CRDTs Operation-Based. Lecture Notes in Computer Science. Springer Berlin Heidelberg. pp. 126–140. ISBN 9783662433515. doi:10.1007/978-3-662-43352-2_11.
- Burckhardt, S., Gotsman, A., Yang, H., Zawirski, M.: Replicated data types: specification, verification, optimality. In Jagannathan, S., Sewell, P., eds.: POPL, ACM (2014) 271–284

- https://hal.inria.fr/hal-01158370v1/document
- (?) Dotted Version Vectors: Logical Clocks for Optimistic Replication
- (?) (improved dvvs? / related to storage) https://hal.inria.fr/hal-01775033/document
- *** https://queue.acm.org/detail.cfm?id=2917756

Composition of State-based CRDTs
http://haslab.uminho.pt/cbm/files/crdtcompositionreport.pdf

http://archagon.net/blog/2018/03/24/data-laced-with-history/
http://google.github.io/xi-editor/docs/crdt-details.html
bounded counters https://arxiv.org/pdf/1503.09052.pdf

- big sets https://pages.lip6.fr/syncfree/index.php/2-uncategorised/53-big-sets.html

#### Why not OT
http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.53.933&rep=rep1&type=pdf
http://www3.ntu.edu.sg/home/czsun/projects/otfaq/#_Toc321146192
http://google.github.io/xi-editor/docs/crdt-details.html
"On Consistency of Operational Transformation Approach" https://arxiv.org/pdf/1302.3292.pdf

#### Why CRDTs API over "simple" REST API
- https://writings.quilt.org/2014/05/12/distributed-systems-and-the-end-of-the-api/

### Inspiration
https://github.com/jemc/pony-crdt/
https://github.com/SyncFree/SwiftCloud/
https://github.com/CBaquero/delta-enabled-crdts/
https://github.com/ipfs-shipyard/js-delta-crdts/