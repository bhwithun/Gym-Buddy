/**
 * Estimated history from 2020-01-01 through today, 1 hour per visit.
 * 2020: Tue/Thu only. 2021 onward: Mon/Tue/Thu/Fri.
 * Off: first week of July (1–7) and the Sun–Sat week containing Christmas.
 * Does not overwrite KEEP_DATES (real pushes).
 */
import { writeFileSync } from "node:fs";
import { resolve } from "node:path";

const TRAIN_JS_DAYS = new Set([1, 2, 4, 5]); // Mon, Tue, Thu, Fri from 2021
const TRAIN_2020_JS_DAYS = new Set([2, 4]); // Tue, Thu
const DURATION_MS = 60 * 60 * 1000;
const EXERCISE_COUNT = 5;
const SET_COUNT = 16;
const KEEP_DATES = new Set(["2026-09-06"]);
const START = new Date(2020, 0, 1);
const END = new Date();
END.setHours(23, 59, 59, 999);

const KEEP_INDEX = [
  {
    date: "2026-09-06",
    durationMs: 2168714,
    setCount: 17,
    exerciseCount: 5,
    isMakeup: true,
  },
];

function ymd(d) {
  return (
    d.getFullYear() +
    "-" +
    String(d.getMonth() + 1).padStart(2, "0") +
    "-" +
    String(d.getDate()).padStart(2, "0")
  );
}

function isFirstWeekOfJuly(d) {
  return d.getMonth() === 6 && d.getDate() <= 7;
}

function isChristmasWeek(d) {
  const christmas = new Date(d.getFullYear(), 11, 25);
  const weekStart = new Date(christmas);
  weekStart.setDate(25 - christmas.getDay());
  weekStart.setHours(0, 0, 0, 0);
  const weekEnd = new Date(weekStart);
  weekEnd.setDate(weekStart.getDate() + 6);
  weekEnd.setHours(23, 59, 59, 999);
  return d >= weekStart && d <= weekEnd;
}

function androidDayOfWeek(jsDay) {
  return jsDay === 0 ? 1 : jsDay + 1;
}

function isTrainDay(d) {
  const days = d.getFullYear() === 2020 ? TRAIN_2020_JS_DAYS : TRAIN_JS_DAYS;
  return days.has(d.getDay());
}

const bulk = [];
const index = [];
const cursor = new Date(START.getFullYear(), START.getMonth(), START.getDate());

while (cursor <= END) {
  const date = ymd(cursor);
  if (
    isTrainDay(cursor) &&
    !KEEP_DATES.has(date) &&
    !isFirstWeekOfJuly(cursor) &&
    !isChristmasWeek(cursor)
  ) {
    const start = new Date(
      cursor.getFullYear(),
      cursor.getMonth(),
      cursor.getDate(),
      18,
      0,
      0,
    );
    const startMs = start.getTime();
    const record = {
      date,
      isMakeup: false,
      estimated: true,
      dayOfWeek: androidDayOfWeek(cursor.getDay()),
      startMs,
      endMs: startMs + DURATION_MS,
      durationMs: DURATION_MS,
      exerciseCount: EXERCISE_COUNT,
      setCount: SET_COUNT,
      exercises: [],
      receivedAt: new Date().toISOString(),
    };
    bulk.push({ key: `workout:${date}`, value: JSON.stringify(record) });
    index.push({
      date,
      durationMs: DURATION_MS,
      setCount: SET_COUNT,
      exerciseCount: EXERCISE_COUNT,
      isMakeup: false,
      estimated: true,
    });
  }
  cursor.setDate(cursor.getDate() + 1);
}

for (const kept of KEEP_INDEX) {
  if (!index.some((entry) => entry.date === kept.date)) {
    index.push(kept);
  }
}
index.sort((a, b) => a.date.localeCompare(b.date));
bulk.push({ key: "index", value: JSON.stringify(index) });

const outPath = resolve(process.argv[2] ?? "seed-history.bulk.json");
writeFileSync(outPath, JSON.stringify(bulk));
const hours = index.reduce((sum, entry) => sum + entry.durationMs, 0) / 3600000;
console.log(
  JSON.stringify(
    {
      outPath,
      keys: bulk.length,
      workouts: index.length,
      estimated: index.filter((entry) => entry.estimated).length,
      first: index[0]?.date,
      last: index[index.length - 1]?.date,
      hours: Math.round(hours),
    },
    null,
    2,
  ),
);
