export function toToolText(value: unknown) {
  return {
    content: [
      {
        type: "text" as const,
        text: JSON.stringify(value, null, 2)
      }
    ]
  };
}

export function clampLimit(limit: number, max = 100): number {
  if (!Number.isFinite(limit)) {
    return max;
  }
  return Math.max(1, Math.min(Math.trunc(limit), max));
}

export function limitArray<T>(items: T[], limit: number, max = 100): T[] {
  return items.slice(0, clampLimit(limit, max));
}
