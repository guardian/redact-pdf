import 'source-map-support/register';
import { App } from 'aws-cdk-lib';
import { CvRedactTool } from '../lib/cv-redact-tool';

const app = new App();
new CvRedactTool(app, 'CvRedactTool-PROD', {
	stack: 'hiring-and-onboarding',
	stage: 'PROD',
});
