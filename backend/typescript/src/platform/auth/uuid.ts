import { v5 as uuidv5 } from 'uuid';
import { config } from '../config';

/** user_id = UUIDv5(ns, "firebase:<project_id>:<uid>") */
export function firebaseUserId(firebaseProjectId: string, firebaseUid: string): string {
  const providerKey = `firebase:${firebaseProjectId}:${firebaseUid}`;
  return uuidv5(providerKey, config.uuidNamespace);
}
