const MONEY =
  /[₹$€£]|\b(budget|expense|spent|amount|settlement|paid you|your share|invoice|balance)\b|\d+(\.\d+)?%/i;
const MOOD = /\b(mood|feeling|anxious|stressed|sad|happy|lonely|overwhelmed)\b/i;
const SECRET = /bearer\s+[a-z0-9._-]{12,}|eyJ[A-Za-z0-9_-]{12,}|signedUrl|[?&]token=/i;

const GENERIC = {
  title: 'Momentra',
  body: 'You have an update in Momentra.',
};

/**
 * Lock-screen push copy. Money, mood, tokens, and signed URLs stay off the notification shade.
 * In-app history may still use the detailed copy from `notificationCopy`.
 */
export function lockScreenPush(title: string, body: string): { title: string; body: string } {
  const blob = `${title}\n${body}`;
  if (MONEY.test(blob) || MOOD.test(blob) || SECRET.test(blob)) return GENERIC;
  return { title, body };
}
