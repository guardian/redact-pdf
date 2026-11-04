import { describe, expect, it } from 'bun:test';
import { App } from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { CvRedactTool } from './cv-redact-tool';

describe('The CvRedactTool stack', () => {
	it('matches the snapshot', () => {
		const app = new App();
		const stack = new CvRedactTool(app, 'CvRedactTool', {
			stack: 'hiring-and-onboarding',
			stage: 'TEST',
			env: { region: 'eu-west-1' },
		});
		const template = Template.fromStack(stack);
		expect(template.toJSON()).toMatchSnapshot();
	});
});
