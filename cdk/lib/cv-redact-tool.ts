import { join } from "path";
import {GuEc2App} from "@guardian/cdk";
import { AccessScope } from '@guardian/cdk/lib/constants';
import type { GuStackProps } from "@guardian/cdk/lib/constructs/core";
import { GuStack } from "@guardian/cdk/lib/constructs/core";
import type { AppIdentity } from '@guardian/cdk/lib/constructs/core/identity';
import { GuCname } from '@guardian/cdk/lib/constructs/dns';
import { GuardianPublicNetworks } from '@guardian/private-infrastructure-config';
import { Duration } from 'aws-cdk-lib';
import type { App } from "aws-cdk-lib";
import {
  InstanceClass,
  InstanceSize,
  InstanceType,
  Peer,
} from 'aws-cdk-lib/aws-ec2';
import { CfnInclude } from "aws-cdk-lib/cloudformation-include";


export class CvRedactTool extends GuStack {
  
  private static app: AppIdentity = {
    app: 'cv-redact-tool',
  };

  constructor(scope: App, id: string, props: GuStackProps) {
    super(scope, id, props);

    const domainName = 'cv-redact.gutools.co.uk';  

    const ec2App = new GuEc2App(this, {
      applicationPort: 9000,
      app: 'cv-redact-tool',
      access: {
        scope: AccessScope.RESTRICTED,
        cidrRanges: [Peer.ipv4(GuardianPublicNetworks.London)],
      },
      instanceType: InstanceType.of(InstanceClass.T4G, InstanceSize.MEDIUM),
      certificateProps:{
        domainName,
      },
      monitoringConfiguration: {
        noMonitoring: true,
      },
      userData: {
        distributable: {
          fileName: "cv-redact-tool.deb",
          executionStatement: `dpkg -i /cv-redact-tool/cv-redact-tool.deb`,
        }
      },
      scaling: {
          minimumInstances: 1,
          maximumInstances: 2,
      },
    });

    new GuCname(this, 'cv-redact.gutools.co.uk - cert', {
      app: CvRedactTool.app.app,
      domainName,
      ttl: Duration.hours(1),
      resourceRecord: ec2App.loadBalancer.loadBalancerDnsName,
    });

  }
}