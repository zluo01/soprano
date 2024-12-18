import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * turn second into normal time
 * @param seconds  input seconds
 * @returns {string}
 */
export function format(seconds: number): string {
  return Number.isNaN(seconds)
    ? '0:00'
    : (seconds - (seconds %= 60)) / 60 + (9 < seconds ? ':' : ':0') + seconds;
}

export function formatTime(seconds: number): string {
  const hours = Math.floor(seconds / 3600);
  seconds %= 3600; // remaining seconds after hours are extracted
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;

  let formattedTime = '';

  if (hours > 0) {
    formattedTime += `${hours}h `;
  }

  if (minutes > 0 || hours > 0) {
    formattedTime += `${minutes}m `;
  }

  formattedTime += `${remainingSeconds}s`;

  return formattedTime.trim();
}
