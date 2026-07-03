<script setup>
// OnboardingGuide — static documentation page, no API calls
</script>

<template>
  <div class="onboarding-page">
    <div class="page-header">
      <h1 class="page-title">Onboarding Guide</h1>
      <p class="page-subtitle">Everything you need to know about the Identity Entitlement Broker</p>
    </div>

    <!-- Section 1 -->
    <div class="guide-card">
      <div class="card-icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10" /><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" /><line x1="12" y1="17" x2="12.01" y2="17" />
        </svg>
      </div>
      <h2>What is Identity Entitlement Broker?</h2>
      <p>
        The Identity Entitlement Broker is a central policy decision point (PDP) that decouples identity
        management from application authorization. It acts as an intermediary between your identity providers
        (IdPs) and your applications, translating identity claims into granular entitlements.
      </p>
      <p>
        By consolidating identity resolution, role mapping, and policy evaluation into a single service, the
        broker provides a unified authorization layer that works across all your applications — regardless of
        whether they use OIDC, SAML, LDAP, or custom authentication.
      </p>
      <div class="highlight-box">
        <strong>Key Capabilities:</strong>
        <ul>
          <li>Multi-tenant identity management with tenant isolation</li>
          <li>Pluggable identity provider support (OIDC, SAML, LDAP, OAuth2)</li>
          <li>SCIM v2.0 user and group provisioning</li>
          <li>Dynamic role mapping from identity claims and group membership</li>
          <li>Fine-grained entitlement resolution and policy evaluation</li>
          <li>Comprehensive audit logging of all authorization decisions</li>
        </ul>
      </div>
    </div>

    <!-- Section 2 -->
    <div class="guide-card">
      <div class="card-icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
        </svg>
      </div>
      <h2>SSO Onboarding Guide</h2>
      <p>Follow these steps to configure Single Sign-On (SSO) for a tenant:</p>
      <ol class="steps-list">
        <li>
          <strong>Create a Tenant</strong>
          <p>Navigate to <strong>Tenants</strong> and click <strong>+ Add Tenant</strong>. Provide a name and slug. The slug should be URL-friendly (e.g., <code>acme-corp</code>).</p>
        </li>
        <li>
          <strong>Configure an IdP Connection</strong>
          <p>Open the tenant detail page, go to the <strong>IdP Connections</strong> tab, and click <strong>+ Add Connection</strong>. Select your provider type (OIDC, SAML, or LDAP) and enter the issuer URL, client ID, and client secret.</p>
        </li>
        <li>
          <strong>Define Role Mappings</strong>
          <p>Navigate to <strong>Role Mappings</strong> and create mappings that translate IdP claims (like <code>groups:admin</code>) to application roles (like <code>administrator</code>). Set priorities to control evaluation order.</p>
        </li>
        <li>
          <strong>Verify SSO</strong>
          <p>Use the <strong>Policy Tester</strong> to simulate access requests and verify that policy decisions evaluate correctly based on your configuration.</p>
        </li>
      </ol>
    </div>

    <!-- Section 3 -->
    <div class="guide-card">
      <div class="card-icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
        </svg>
      </div>
      <h2>Configuring Your IdP</h2>

      <h3>OpenID Connect (OIDC)</h3>
      <p>
        For OIDC providers (Auth0, Okta, Keycloak, Azure AD, Google Workspace), configure a confidential
        client application with:
      </p>
      <ul>
        <li><strong>Redirect URI:</strong> <code>https://your-broker.example.com/api/auth/callback</code></li>
        <li><strong>Grant type:</strong> Authorization Code flow with PKCE</li>
        <li><strong>Scopes:</strong> <code>openid profile email</code> (plus any custom claims you need)</li>
        <li><strong>Client authentication:</strong> Client secret (preferred) or private key JWT</li>
      </ul>
      <p>
        The broker validates the ID token, extracts claims (sub, email, groups, roles), and uses them for
        role mapping and entitlement resolution.
      </p>

      <h3>SAML 2.0</h3>
      <p>
        For SAML providers (Okta, OneLogin, Azure AD, ADFS), configure a service provider (SP) application:
      </p>
      <ul>
        <li><strong>ACS URL (Assertion Consumer Service):</strong> <code>https://your-broker.example.com/api/auth/saml/callback</code></li>
        <li><strong>Entity ID:</strong> <code>https://your-broker.example.com/api/auth/saml/metadata</code></li>
        <li><strong>Name ID format:</strong> <code>urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress</code></li>
        <li><strong>Attribute mapping:</strong> Map SAML attributes (like <code>groups</code>, <code>roles</code>) to the broker's claim format</li>
      </ul>
      <p>
        Upload your IdP metadata XML to the broker or configure it via the admin console. The broker handles
        assertion validation, signature verification, and session management.
      </p>
    </div>

    <!-- Section 4 -->
    <div class="guide-card">
      <div class="card-icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" />
        </svg>
      </div>
      <h2>SCIM Provisioning</h2>
      <p>
        The broker implements the <strong>System for Cross-domain Identity Management (SCIM) 2.0</strong>
        protocol (<a href="https://www.rfc-editor.org/rfc/rfc7644" target="_blank">RFC 7644</a>) for
        automated user and group provisioning.
      </p>

      <h3>SCIM Endpoints</h3>
      <ul>
        <li><code>POST /scim/Users</code> — Create a user</li>
        <li><code>GET /scim/Users</code> — List users (supports filtering, pagination)</li>
        <li><code>GET /scim/Users/:id</code> — Get a specific user</li>
        <li><code>PUT /scim/Users/:id</code> — Update a user</li>
        <li><code>PATCH /scim/Users/:id</code> — Partial update a user</li>
        <li><code>DELETE /scim/Users/:id</code> — Delete a user</li>
        <li><code>POST /scim/Groups</code> — Create a group</li>
        <li><code>GET /scim/Groups</code> — List groups</li>
        <li><code>GET /scim/Groups/:id</code> — Get a specific group</li>
        <li><code>PUT /scim/Groups/:id</code> — Update a group</li>
        <li><code>DELETE /scim/Groups/:id</code> — Delete a group</li>
      </ul>

      <h3>How Provisioning Works</h3>
      <ol class="steps-list">
        <li>Your IdP or identity source pushes user/group data via SCIM calls to the broker</li>
        <li>The broker validates the SCIM schema and stores the provisioned resources</li>
        <li>Users and groups are associated with the tenant specified in the request headers or request body</li>
        <li>The broker maintains an internal mapping of SCIM resources to the local identity graph</li>
        <li>When entitlements are resolved, the broker evaluates both directly assigned and group-inherited permissions</li>
      </ol>

      <div class="highlight-box">
        <strong>Note:</strong> SCIM provisioning requires an API token or service account with the
        <code>provision</code> action permission. Configure this via the Policy Tester to verify.
      </div>
    </div>

    <!-- Section 5 -->
    <div class="guide-card">
      <div class="card-icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="20 6 9 17 4 12" />
        </svg>
      </div>
      <h2>Role Mapping</h2>
      <p>
        Role mapping defines how identity claims from your IdP translate to application roles within the broker.
        Each mapping specifies:
      </p>
      <ul>
        <li><strong>Source Type:</strong> Where the claim comes from (<code>claim</code>, <code>group</code>, or <code>attribute</code>)</li>
        <li><strong>Source Value:</strong> The expected value of the claim (e.g., <code>admin</code>, <code>group:engineering</code>)</li>
        <li><strong>Target Role:</strong> The role to assign when the source condition matches (e.g., <code>administrator</code>, <code>viewer</code>)</li>
        <li><strong>Priority:</strong> Controls evaluation order. Lower numbers are evaluated first. Higher priority mappings can override lower ones.</li>
      </ul>

      <h3>Mapping Evaluation</h3>
      <p>
        When a user authenticates, the broker:
      </p>
      <ol class="steps-list">
        <li>Extracts all claims and group memberships from the IdP token</li>
        <li>Iterates through role mappings sorted by priority (ascending)</li>
        <li>For each mapping, checks if the source type and value match any of the user's claims</li>
        <li>If matched, assigns the target role to the user</li>
        <li>If multiple mappings match, all matching roles are assigned to the user</li>
      </ol>
    </div>

    <!-- Section 6 -->
    <div class="guide-card">
      <div class="card-icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
        </svg>
      </div>
      <h2>Entitlement Resolution</h2>
      <p>
        Entitlements are fine-grained permissions that determine what actions a user can perform on specific
        resources. The broker's entitlement resolution follows a layered model:
      </p>

      <h3>Entitlement Sources</h3>
      <ul>
        <li><strong>Direct Assignment:</strong> Entitlements assigned directly to a user</li>
        <li><strong>Group Inheritance:</strong> Entitlements assigned to groups the user belongs to</li>
        <li><strong>Role-Based:</strong> Entitlements derived from the user's roles through role-entitlement mappings</li>
        <li><strong>Product-Based:</strong> Entitlements organized under products, allowing bulk assignment</li>
      </ul>

      <h3>Resolution Order</h3>
      <p>
        When resolving a user's effective entitlements, the broker:
      </p>
      <ol class="steps-list">
        <li>Collects all direct user entitlements</li>
        <li>Resolves group membership and collects group-level entitlements</li>
        <li>Resolves role mappings from the user's claims and collects role-based entitlements</li>
        <li>Merges all sources, removing duplicates</li>
        <li>Returns the consolidated list of effective entitlements</li>
      </ol>

      <p>
        Use the <strong>Entitlements</strong> section to manage products, create entitlements, and assign them
        to users or groups. The <strong>Effective Entitlements Lookup</strong> tool lets you verify what
        entitlements a specific user actually has.
      </p>
    </div>

    <!-- Section 7 -->
    <div class="guide-card">
      <div class="card-icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
          <path d="M9 12l2 2 4-4" />
        </svg>
      </div>
      <h2>Policy Decision Model</h2>
      <p>
        The broker uses a policy-based access control (PBAC) model for making authorization decisions.
        Every access request is evaluated against a set of configurable policies.
      </p>

      <h3>Decision Flow</h3>
      <ol class="steps-list">
        <li><strong>Request:</strong> An actor (user or service) requests to perform an action on a resource</li>
        <li><strong>Authentication:</strong> The broker verifies the actor's identity (via API token, JWT, or session)</li>
        <li><strong>Entitlement Resolution:</strong> The broker resolves the actor's effective entitlements</li>
        <li><strong>Policy Evaluation:</strong> Each policy rule is evaluated against the request context (actor, action, resource, environment)</li>
        <li><strong>Decision:</strong> The first matching rule determines the decision — <strong class="text-success">ALLOW</strong> or <strong class="text-danger">DENY</strong></li>
        <li><strong>Audit:</strong> Every decision is logged with full context for audit and compliance</li>
      </ol>

      <h3>Policy Rules</h3>
      <p>
        Each policy rule consists of:
      </p>
      <ul>
        <li><strong>Effect:</strong> <code>allow</code> or <code>deny</code></li>
        <li><strong>Subjects:</strong> The users, groups, or roles the rule applies to</li>
        <li><strong>Actions:</strong> The actions being authorized (e.g., <code>access</code>, <code>manage</code>, <code>impersonate</code>, <code>provision</code>)</li>
        <li><strong>Resources:</strong> The resources being accessed (e.g., <code>/api/v1/users</code>, <code>app:dashboard</code>)</li>
        <li><strong>Conditions:</strong> Additional context constraints (e.g., IP range, time of day, device type)</li>
      </ul>

      <div class="highlight-box">
        <strong>Important:</strong> The broker uses a <em>deny-by-default</em> model. If no policy rule matches,
        access is denied. Always test your policies using the <strong>Policy Tester</strong> before deploying
        to production.
      </div>

      <h3>Example Policy</h3>
      <pre class="code-block">{
  "effect": "allow",
  "subjects": ["role:administrator"],
  "actions": ["access", "manage"],
  "resources": ["/api/*"],
  "conditions": {
    "ip_range": ["10.0.0.0/8"],
    "mfa_required": true
  }
}</pre>
    </div>
  </div>
</template>

<style scoped>
.onboarding-page {
  max-width: 800px;
}

.page-header {
  margin-bottom: 2rem;
}

.page-title {
  font-size: 1.5rem;
  margin-bottom: 0.25rem;
}

.page-subtitle {
  color: var(--color-text-secondary);
  font-size: 0.95rem;
}

.guide-card {
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 1.5rem;
  margin-bottom: 1.25rem;
}

.card-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background-color: var(--color-accent-light);
  color: var(--color-accent-text);
  border-radius: var(--radius-lg);
  margin-bottom: 1rem;
}

.guide-card h2 {
  font-size: 1.2rem;
  margin-bottom: 1rem;
}

.guide-card h3 {
  font-size: 1rem;
  color: var(--color-accent-text);
  margin-top: 1.25rem;
  margin-bottom: 0.5rem;
}

.guide-card p {
  color: var(--color-text-secondary);
  line-height: 1.7;
  margin-bottom: 0.75rem;
}

.guide-card ul,
.guide-card ol {
  padding-left: 1.25rem;
  margin-bottom: 1rem;
}

.guide-card li {
  color: var(--color-text-secondary);
  line-height: 1.7;
  margin-bottom: 0.35rem;
}

.guide-card a {
  color: var(--color-accent-text);
}

.guide-card a:hover {
  text-decoration: underline;
}

.guide-card code {
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
  background-color: var(--color-bg-primary);
  padding: 0.15rem 0.35rem;
  border-radius: var(--radius-sm);
  font-size: 0.85em;
  color: var(--color-accent-text);
  border: 1px solid var(--color-border);
}

.steps-list {
  counter-reset: step;
  list-style: none;
  padding-left: 0;
}

.steps-list li {
  counter-increment: step;
  position: relative;
  padding-left: 2.5rem;
  padding-bottom: 1rem;
  margin-bottom: 0.5rem;
  border-left: 2px solid var(--color-border);
}

.steps-list li:last-child {
  border-left-color: transparent;
  padding-bottom: 0;
}

.steps-list li::before {
  content: counter(step);
  position: absolute;
  left: -14px;
  top: 0;
  width: 26px;
  height: 26px;
  background-color: var(--color-accent);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 600;
}

.steps-list li strong {
  display: block;
  color: var(--color-text-primary);
  margin-bottom: 0.25rem;
}

.steps-list li p {
  margin-bottom: 0;
}

.highlight-box {
  background-color: var(--color-accent-light);
  border: 1px solid rgba(59, 130, 246, 0.3);
  border-radius: var(--radius-md);
  padding: 1rem 1.25rem;
  margin-top: 1rem;
}

.highlight-box strong {
  color: var(--color-accent-text);
  display: block;
  margin-bottom: 0.35rem;
}

.highlight-box ul {
  margin-bottom: 0;
}

.text-success {
  color: var(--color-success);
}

.text-danger {
  color: var(--color-danger);
}

.code-block {
  background-color: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1rem;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
  font-size: 0.82rem;
  line-height: 1.5;
  overflow-x: auto;
  color: var(--color-text-primary);
  margin-top: 0.75rem;
}
</style>
