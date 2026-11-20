# CSC 311 – Costco Checkout Simulator (Producer–Consumer Lab)

**Course:** CSC 311, Dr. Alrajab, Module 12 Lab
**Starter code:** [CSC311_Example_Threads_2](https://github.com/moaathalrajab/CSC311_Example_Threads_2), `ex2` package
**Shared repo:** [christianpivaral/Costco_Checkout_Simulator](https://github.com/christianpivaral/Costco_Checkout_Simulator)

## Team members and contributions

| Member | Contribution |
|---|---|
| **Christian Pivaral** | Created the shared GitHub repository and added collaborators. Wrote `CheckoutQueue` (the shared `ArrayBlockingQueue` of customer IDs, blocking `join`/`next`, and the closing "poison pill"), plus the helper classes `Clock`, `Log` and `SimulationResult`. |
| **Logan Raycraft** | Wrote `Customer`, the producer: shops for a random time, then joins the checkout line (blocking while the line is full). |
| **Sahiti Attada** | Wrote `Cashier`, the consumer: repeatedly takes the next customer from the line, serves them, and records wait times and busy time. |
| **All three** | Built the main class `CostcoSimulation` and the `Experiments` runner together, ran the five experiments, and wrote the staffing analysis and recommendation below. |

The original `Ex1`–`ex4` example packages are unchanged.

---

## From `ex2` to Costco

| `ex2` (numbers) | `costco` (checkout) | Role |
|---|---|---|
| `Producer` puts 1–10 into the buffer | `Customer` shops for a random time, then joins the line | Producer |
| `Consumer` takes values and sums them | `Cashier` takes the next customer ID and rings them up | Consumer |
| `BlockingBuffer` (`ArrayBlockingQueue<Integer>` of size 1) | `CheckoutQueue` (`ArrayBlockingQueue<Integer>` of customer IDs, capacity chosen by user) | Shared buffer |
| `BlockingBufferTest` | `CostcoSimulation` (prompts for parameters and prints a report) | Main |

Supporting classes:
- `Clock` speeds up time: 1 simulated minute = 50 ms, so an hour runs in about 3 seconds.
- `Log` prints events and can be switched off for batch runs.
- `SimulationResult` is a record that holds one run's statistics and computes labor cost.
- `Experiments` runs the five required experiments and the 60-customers/hour staffing analysis.

### How the simulation works
- Customers finish shopping at random times spread over the arrival window. For example, 60 customers at 60/hour arrive over 60 minutes, about 1 per minute.
- All cashiers share **one line**, like Costco's single line feeding several registers. Each customer takes 1–4 minutes to ring up (2.5 minutes on average).
- **Wait time** runs from "done shopping" to "a cashier starts serving me". It includes any time spent stuck because the line was full.
- **Closing time:** once every customer is in line, the main thread puts one `CLOSED` (-1) marker per cashier into the queue (a "poison pill"). Each cashier stops when it takes one. The marker sits behind the real customers, so everyone still in line gets served first.
- **Labor cost** = cashiers × $/hour × (total minutes ÷ 60). Every cashier is paid for the whole time the store runs, busy or not.

### Why synchronization matters
In `Ex1` (`UnsynchronizedBuffer`), producer and consumer read and write a shared `int` with no coordination. The consumer can read the same value twice or miss values. That is a **race condition**. Here it would mean two cashiers serving the same customer, or a customer disappearing from the line.

`ArrayBlockingQueue` prevents this because it is thread-safe:
- `put()` and `take()` are atomic, so exactly one cashier gets each customer.
- `put()` **blocks** when the line is full: the customer waits for space.
- `take()` **blocks** when the line is empty: the cashier waits for a customer.

Because the queue does the locking, the `costco` classes need no `synchronized` methods, `wait()` or `notifyAll()`. The shared wait-time list uses `Collections.synchronizedList`, and the counters use `AtomicInteger`/`ConcurrentHashMap` for the same reason.

---

## How to run

**IntelliJ:** open the project, then run `costco.CostcoSimulation` (interactive) or `costco.Experiments` (all experiments, takes about 2–3 minutes).

**Command line:**
```bash
javac -d out src/costco/*.java
java -cp out costco.CostcoSimulation   # prompts; press Enter to accept a default
java -cp out costco.Experiments
```

Example prompts:
```
Number of customers [20]:
Number of cashiers [3]:
Checkout line capacity [10]:
Customer arrival rate (customers/hour) [60]:
Cashier wage ($/hour) [18.00]:
```

---

## Experiments

Each row is the average of 5 runs. Random shopping/service times and thread scheduling make single runs noisy, so your numbers will differ a little.
*Full* = customers who found the line at capacity and had to wait to get in.

### Part 1: Five experiments (20 customers arriving at 60/hour, $18/hour)

| Scenario | Avg wait | Max wait | Total time | Cashier utilization | Found line full | Labor cost | $/customer |
|---|---|---|---|---|---|---|---|
| 1. Understaffed: 1 cashier, cap 10 | 14.1 min | 26.6 min | 48.9 min | 99% | 2.6 | $14.68 | $0.73 |
| 2. Optimal: 3 cashiers, cap 10 | 0.9 min | 3.3 min | 23.5 min | 77% | 0 | $21.13 | $1.06 |
| 3. Overstaffed: 10 cashiers, cap 10 | 0.0 min | 0.0 min | 22.4 min | 22% | 0 | $67.17 | $3.36 |
| 4. Small line: 3 cashiers, cap 2 | 0.9 min | 3.2 min | 22.6 min | 75% | 3.2 | $20.33 | $1.02 |
| 5. Large line: 3 cashiers, cap 50 | 0.7 min | 2.3 min | 23.5 min | 74% | 0 | $21.18 | $1.06 |

**Observations**
1. **Understaffed:** customers arrive about 1 per minute, but one cashier needs 2.5 minutes each. The line keeps growing, waits reach almost half an hour, and the cashier is busy 99% of the time. It looks cheapest per customer, but it is a bad customer experience, and the store stays open more than twice as long to clear the line.
2. **Optimal:** 3 cashiers can serve about 72 customers/hour (3 × 60 ÷ 2.5) against 60 arriving. Waits stay around a minute and cashiers are busy about 75–80% of the time.
3. **Overstaffed:** nobody waits, but cashiers are idle about 78% of the time and labor cost triples. The total time barely changes because it is limited by when customers finish shopping, not by cashiers.
4. **Small line (cap 2):** some customers found the line full and their `put()` blocked until a spot opened. With enough cashiers this barely changed the average wait. The blocked time just moved from "in line" to "waiting to get in line". In a real store this is people crowding the aisles.
5. **Large line (cap 50):** a bigger line did not shorten waits; only more cashiers do that. Line capacity only matters when the line actually fills up, which happens when the store is understaffed.

**Main takeaway:** the ratio of service capacity to arrival rate (utilization) drives wait times. Queue size only decides *where* customers wait.

### Part 2: Staffing for 60 customers/hour at $18/hour (60 customers, cap 20)

| Cashiers | Avg wait | Max wait | Total time | Utilization | Labor cost | Cost/hour of staff |
|---|---|---|---|---|---|---|
| 1 | 44.0 min | 87.4 min | 149.3 min | 99% | $44.79 | $18 |
| 2 | 10.1 min | 19.4 min | 78.2 min | 96% | $46.93 | $36 |
| **3** | **1.0 min** | **3.4 min** | **62.2 min** | **80%** | **$55.96** | **$54** |
| 4 | 0.2 min | 1.8 min | 62.3 min | 58% | $74.80 | $72 |
| 5 | 0.0 min | 0.7 min | 61.5 min | 48% | $92.29 | $90 |
| 6 | 0.0 min | 0.6 min | 61.7 min | 40% | $111.01 | $108 |

### Recommendation: **3 cashiers ($54/hour)**

- **1–2 cashiers cannot keep up.** Their capacity (24 and 48 customers/hour) is below the 60/hour arrival rate, so the line grows all hour. With 2 cashiers the average wait is already about 10 minutes, and it would keep getting worse in a longer shift. The labor savings are not worth losing customers.
- **3 cashiers** is the smallest staff with capacity (72/hour) above demand. The average wait is about 1 minute, the worst case is about 3 minutes, and cashiers are busy about 80% of the time.
- **4+ cashiers** costs another $18/hour each but saves less than a minute of average wait, and utilization falls below 60%.

A reasonable policy is 3 cashiers as the base, with a 4th opened at rush times (when arrivals go above ~70/hour) or when the line passes a set length.

**Limitations:** the model uses uniform random arrivals and service times, one shared line, and fixed staffing. Real stores have bursts (lunch, weekends), different basket sizes and self-checkout. Because the simulation runs on real threads, its timing also depends on the OS scheduler, so results vary a little between runs.
