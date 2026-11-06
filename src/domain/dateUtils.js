/**
 * Domain Utilities: Date and Time operations
 */

export function getFormattedDate(daysOffset = 0) {
  const target = new Date(Date.now() + daysOffset * 86400000);
  return target.toISOString().slice(0, 10);
}

export function getCurrentUtcTime() {
  return new Date().toTimeString().slice(0, 8) + ' UTC';
}

