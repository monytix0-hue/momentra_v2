import { initSentry } from './platform/observability/sentry';
import { startServer } from './app';

initSentry();
startServer();
