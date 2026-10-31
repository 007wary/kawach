// Same rule as app/.../family/Mask.kt, applied again on the server in case an older app sends unmasked text.
// Any run of 6+ digits (single spaces or dashes allowed between them) keeps only its last 2 digits.
export function maskNumbers(text) {
  return text.replace(/\d(?:[ -]?\d){5,}/g, (match) => {
    const digits = match.replace(/\D/g, "").length;
    let seen = 0;
    return match.replace(/\d/g, (d) => (++seen > digits - 2 ? d : "•"));
  });
}
