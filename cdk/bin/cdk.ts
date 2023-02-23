import 'source-map-support/register';
import { GuRootExperimental } from '@guardian/cdk/lib/experimental/constructs';
import { CvRedactTool } from '../lib/cv-redact-tool';

const app = new GuRootExperimental();
new CvRedactTool(app, 'CvRedactTool-PROD', {
	stack: 'hiring-and-onboarding',
	stage: 'PROD',
	env: {
		region: 'eu-west-1',
	},
});
