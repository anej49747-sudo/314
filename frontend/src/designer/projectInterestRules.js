// The portfolio API provides aggregate integers. Missing values render as zero.
export function interestMetricCount(value) {
  return Number.isSafeInteger(value) && value >= 0 ? value : 0
}
