## TC1  (Q = 3, CS = 0)

Input: P1(arr 0, burst 4, prio 2), P2(arr 1, burst 7, prio 3), P3(arr 3, burst 2, prio 1)

**SJF**

| Process | Arrival | Burst | Finish | Turnaround | Waiting | Response |
|---|---|---|---|---|---|---|
| P1 | 0 | 4 | 4 | 4 | 0 | 0 |
| P2 | 1 | 7 | 13 | 12 | 5 | 5 |
| P3 | 3 | 2 | 6 | 3 | 1 | 1 |
| **Average** | | | | **6.33** | **2.00** | **2.00** |

**ACS**

| Process | Arrival | Burst | Finish | Turnaround | Waiting | Response |
|---|---|---|---|---|---|---|
| P1 | 0 | 4 | 9 | 9 | 5 | 0 |
| P2 | 1 | 7 | 13 | 12 | 5 | 2 |
| P3 | 3 | 2 | 8 | 5 | 3 | 3 |
| **Average** | | | | **8.67** | **4.33** | **1.67** |

## TC2  (Q = 3, CS = 1)

Input: P1(arr 0, burst 5, prio 4), P2(arr 1, burst 6, prio 2), P3(arr 4, burst 4, prio 3)

**SJF**

| Process | Arrival | Burst | Finish | Turnaround | Waiting | Response |
|---|---|---|---|---|---|---|
| P1 | 0 | 5 | 5 | 5 | 0 | 0 |
| P2 | 1 | 6 | 17 | 16 | 10 | 10 |
| P3 | 4 | 4 | 10 | 6 | 2 | 2 |
| **Average** | | | | **9.00** | **4.00** | **4.00** |

**ACS**

| Process | Arrival | Burst | Finish | Turnaround | Waiting | Response |
|---|---|---|---|---|---|---|
| P1 | 0 | 5 | 18 | 18 | 13 | 0 |
| P2 | 1 | 6 | 15 | 14 | 8 | 3 |
| P3 | 4 | 4 | 20 | 16 | 12 | 4 |
| **Average** | | | | **16.00** | **11.00** | **2.33** |

## TC3  (Q = 4, CS = 1)

Input: P1(arr 0, burst 2, prio 3), P2(arr 1, burst 3, prio 2), P3(arr 2, burst 1, prio 4), P4(arr 3, burst 2, prio 1), P5(arr 4, burst 3, prio 5), P6(arr 5, burst 2, prio 2)

**SJF**

| Process | Arrival | Burst | Finish | Turnaround | Waiting | Response |
|---|---|---|---|---|---|---|
| P1 | 0 | 2 | 2 | 2 | 0 | 0 |
| P2 | 1 | 3 | 14 | 13 | 10 | 10 |
| P3 | 2 | 1 | 4 | 2 | 1 | 1 |
| P4 | 3 | 2 | 7 | 4 | 2 | 2 |
| P5 | 4 | 3 | 18 | 14 | 11 | 11 |
| P6 | 5 | 2 | 10 | 5 | 3 | 3 |
| **Average** | | | | **6.67** | **4.50** | **4.50** |

**ACS**

| Process | Arrival | Burst | Finish | Turnaround | Waiting | Response |
|---|---|---|---|---|---|---|
| P1 | 0 | 2 | 2 | 2 | 0 | 0 |
| P2 | 1 | 3 | 6 | 5 | 2 | 2 |
| P3 | 2 | 1 | 8 | 6 | 5 | 5 |
| P4 | 3 | 2 | 11 | 8 | 6 | 6 |
| P5 | 4 | 3 | 15 | 11 | 8 | 8 |
| P6 | 5 | 2 | 18 | 13 | 11 | 11 |
| **Average** | | | | **7.50** | **5.33** | **5.33** |

## TC4  (Q = 3, CS = 1)

Input: P1(arr 0, burst 12, prio 3), P2(arr 0, burst 10, prio 1), P3(arr 1, burst 15, prio 2), P4(arr 2, burst 8, prio 6), P5(arr 3, burst 9, prio 4)

**SJF**

| Process | Arrival | Burst | Finish | Turnaround | Waiting | Response |
|---|---|---|---|---|---|---|
| P1 | 0 | 12 | 42 | 42 | 30 | 30 |
| P2 | 0 | 10 | 10 | 10 | 0 | 0 |
| P3 | 1 | 15 | 58 | 57 | 42 | 42 |
| P4 | 2 | 8 | 19 | 17 | 9 | 9 |
| P5 | 3 | 9 | 29 | 26 | 17 | 17 |
| **Average** | | | | **30.40** | **19.60** | **19.60** |

**ACS**

| Process | Arrival | Burst | Finish | Turnaround | Waiting | Response |
|---|---|---|---|---|---|---|
| P1 | 0 | 12 | 37 | 37 | 25 | 0 |
| P2 | 0 | 10 | 27 | 27 | 17 | 4 |
| P3 | 1 | 15 | 50 | 49 | 34 | 7 |
| P4 | 2 | 8 | 63 | 61 | 53 | 10 |
| P5 | 3 | 9 | 57 | 54 | 45 | 13 |
| **Average** | | | | **45.60** | **34.80** | **6.80** |

## TC5  (Q = 4, CS = 1)

Input: P1(arr 0, burst 14, prio 2), P2(arr 1, burst 10, prio 3), P3(arr 12, burst 2, prio 1), P4(arr 15, burst 3, prio 1), P5(arr 18, burst 2, prio 1)

**SJF**

| Process | Arrival | Burst | Finish | Turnaround | Waiting | Response |
|---|---|---|---|---|---|---|
| P1 | 0 | 14 | 36 | 36 | 22 | 0 |
| P2 | 1 | 10 | 12 | 11 | 1 | 1 |
| P3 | 12 | 2 | 15 | 3 | 1 | 1 |
| P4 | 15 | 3 | 19 | 4 | 1 | 1 |
| P5 | 18 | 2 | 22 | 4 | 2 | 2 |
| **Average** | | | | **11.60** | **5.40** | **1.00** |

**ACS**

| Process | Arrival | Burst | Finish | Turnaround | Waiting | Response |
|---|---|---|---|---|---|---|
| P1 | 0 | 14 | 20 | 20 | 6 | 0 |
| P2 | 1 | 10 | 37 | 36 | 26 | 4 |
| P3 | 12 | 2 | 23 | 11 | 9 | 9 |
| P4 | 15 | 3 | 27 | 12 | 9 | 9 |
| P5 | 18 | 2 | 30 | 12 | 10 | 10 |
| **Average** | | | | **18.20** | **12.00** | **6.40** |

## TC6  (Q = 3, CS = 2)

Input: P1(arr 0, burst 3, prio 1), P2(arr 0, burst 5, prio 2), P3(arr 15, burst 4, prio 3), P4(arr 15, burst 2, prio 1), P5(arr 16, burst 6, prio 2)

**SJF**

| Process | Arrival | Burst | Finish | Turnaround | Waiting | Response |
|---|---|---|---|---|---|---|
| P1 | 0 | 3 | 3 | 3 | 0 | 0 |
| P2 | 0 | 5 | 10 | 10 | 5 | 5 |
| P3 | 15 | 4 | 23 | 8 | 4 | 4 |
| P4 | 15 | 2 | 17 | 2 | 0 | 0 |
| P5 | 16 | 6 | 31 | 15 | 9 | 9 |
| **Average** | | | | **7.60** | **3.60** | **3.60** |

**ACS**

| Process | Arrival | Burst | Finish | Turnaround | Waiting | Response |
|---|---|---|---|---|---|---|
| P1 | 0 | 3 | 3 | 3 | 0 | 0 |
| P2 | 0 | 5 | 10 | 10 | 5 | 5 |
| P3 | 15 | 4 | 30 | 15 | 11 | 0 |
| P4 | 15 | 2 | 22 | 7 | 5 | 5 |
| P5 | 16 | 6 | 35 | 19 | 13 | 8 |
| **Average** | | | | **10.80** | **6.80** | **3.60** |
