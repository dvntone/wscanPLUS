export const PORTAL_VARIANTS = [
  {
    id: 'operator-generic',
    title: 'Operator-class captive portal',
    artifactId: 'wscanplus-portal-test-001',
    fakeScore: 0.96,
    summary: 'Generic ISP-style portal artifact used to validate fake-versus-real portal analysis without copying a real provider brand.',
    signals: [
      'data-wscanplus-artifact present on <html>',
      'POSTs credentials or guest acceptance to /capture',
      'Password field uses type=text instead of type=password',
      'No CSRF token or server session token in the form',
      'No redirect_uri or MAC hint fields',
      'Expected local-IP serving context instead of an operator domain',
      'Generic terms without operator identity or support details',
    ],
  },
  {
    id: 'future-apartment',
    title: 'Future apartment / library variants',
    artifactId: 'pending-variant-family',
    fakeScore: 0.96,
    summary: 'Follow the same detection contract: preserve wscanplus fingerprint markers while shifting only the visual class.',
    signals: [
      'Keep fingerprint JSON and data-wscanplus attributes intact',
      'Keep /capture endpoint and device analytics payload',
      'Only change visual class, not truth-model semantics',
    ],
  },
];

export function formatScore(score) {
  return `${Math.round(score * 100)}% fake confidence`;
}
