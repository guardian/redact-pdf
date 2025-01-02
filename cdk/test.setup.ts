import { mock } from "bun:test";

mock.module("@guardian/cdk/lib/constants/tracking-tag", () => require("@guardian/cdk/lib/constants/__mocks__/tracking-tag"));
