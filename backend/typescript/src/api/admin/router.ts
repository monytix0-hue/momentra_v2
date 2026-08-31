import { Router } from 'express';
import { adminAuthMiddleware } from '../middleware/admin-auth';
import { adminTelemetryRouter } from './telemetry-router';
import { adminGroupExperiencesRouter } from './group-experiences-router';

export const adminRouter = Router();
adminRouter.use(adminAuthMiddleware);
adminRouter.use('/telemetry', adminTelemetryRouter);
adminRouter.use('/group-experiences', adminGroupExperiencesRouter);
