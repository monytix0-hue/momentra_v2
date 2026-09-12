import { Router } from 'express';
import { adminAuthMiddleware } from '../middleware/admin-auth';
import { adminTelemetryRouter } from './telemetry-router';
import { adminGroupExperiencesRouter } from './group-experiences-router';
import { adminLeanAnalyticsRouter } from './lean-analytics-router';
import { adminNotificationsRouter } from './notifications-router';

export const adminRouter = Router();
adminRouter.use(adminAuthMiddleware);
adminRouter.use('/telemetry', adminTelemetryRouter);
adminRouter.use('/group-experiences', adminGroupExperiencesRouter);
adminRouter.use('/lean', adminLeanAnalyticsRouter);
adminRouter.use('/notifications', adminNotificationsRouter);
