import 'source-map-support/register';
import { GuRoot } from '@guardian/cdk/lib/constructs/root';
import { CvRedactTool } from '../lib/cv-redact-tool';

const app = new GuRoot();
new CvRedactTool(app, 'CvRedactTool-PROD', {
	stack: 'hiring-and-onboarding',
	stage: 'PROD',
	env: {
		region: 'eu-west-1',
	},
});
