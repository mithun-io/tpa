/**
 * generate-claim-pdfs.js
 * Generates two PDFs using only Node.js built-in modules (no external deps).
 * Uses raw PDF 1.4 specification — produces valid, OCR-ready files.
 */
const fs = require('fs');
const path = require('path');

const OUT_DIR = path.join(__dirname, 'test-documents');
if (!fs.existsSync(OUT_DIR)) fs.mkdirSync(OUT_DIR, { recursive: true });

/* ──────────────────────────────────────────────────────────────
   Tiny PDF builder — supports text, lines, rectangles
   ────────────────────────────────────────────────────────────── */
class PdfBuilder {
  constructor(w = 595, h = 842) { // A4 points
    this.w = w; this.h = h;
    this.objs = [];          // [ string ]  — raw PDF objects
    this.pages = [];         // page stream references
  }

  /* ── raw object helpers ── */
  _obj(content) {
    const n = this.objs.length + 1;
    this.objs.push({ n, content });
    return n;
  }

  /* ── encode a page's content stream ── */
  addPage(ops) {
    const stream = ops.join('\n');
    const len = Buffer.byteLength(stream, 'latin1');
    const streamObjNum = this._obj(
      `<< /Length ${len} >>\nstream\n${stream}\nendstream`
    );
    const pageObjNum = this._obj(
      `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${this.w} ${this.h}] /Contents ${streamObjNum} 0 R /Resources << /Font << /F1 4 0 R /F2 5 0 R /F3 6 0 R >> >> >>`
    );
    this.pages.push(pageObjNum);
    return pageObjNum;
  }

  build() {
    // Object 1: catalog (pages ref = obj 2)
    // Object 2: Pages dict
    // Object 3: reserved for pages (filled below)
    // Object 4: Courier  Object 5: Helvetica-Bold  Object 6: Helvetica

    // Reserve slots 1-6
    while (this.objs.length < 6) this.objs.push(null);

    this.objs[0] = { n: 1, content: '<< /Type /Catalog /Pages 2 0 R >>' };
    const kidsStr = this.pages.map(n => `${n} 0 R`).join(' ');
    this.objs[1] = { n: 2, content: `<< /Type /Pages /Kids [${kidsStr}] /Count ${this.pages.length} >>` };
    this.objs[2] = { n: 3, content: '<< >>' }; // placeholder
    this.objs[3] = { n: 4, content: '<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>' };
    this.objs[4] = { n: 5, content: '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>' };
    this.objs[5] = { n: 6, content: '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>' };

    const header = '%PDF-1.4\n%\xE2\xE3\xCF\xD3\n';
    let body = header;
    const offsets = [];

    for (const obj of this.objs) {
      if (!obj) continue;
      offsets[obj.n] = body.length;
      body += `${obj.n} 0 obj\n${obj.content}\nendobj\n`;
    }

    const xrefOffset = body.length;
    const count = this.objs.length + 1;
    let xref = `xref\n0 ${count}\n0000000000 65535 f \n`;
    for (let i = 1; i < count; i++) {
      const off = offsets[i] ?? 0;
      xref += `${String(off).padStart(10, '0')} 00000 n \n`;
    }
    body += xref;
    body += `trailer\n<< /Size ${count} /Root 1 0 R >>\nstartxref\n${xrefOffset}\n%%EOF`;
    return body;
  }
}

/* ── PDF content stream helpers ── */
const esc = s => s.replace(/\\/g, '\\\\').replace(/\(/g, '\\(').replace(/\)/g, '\\)');

function text(x, y, str, font = 'F2', size = 11) {
  return `BT /${font} ${size} Tf ${x} ${y} Td (${esc(str)}) Tj ET`;
}

function line(x1, y1, x2, y2, width = 0.5) {
  return `${width} w ${x1} ${y1} m ${x2} ${y2} l S`;
}

function rect(x, y, w, h, fill = false) {
  return fill
    ? `0.95 g ${x} ${y} ${w} ${h} re f 0 g`
    : `${x} ${y} ${w} ${h} re S`;
}

function grayText(r, g, b) { return `${r} ${g} ${b} rg`; }
const black = '0 0 0 rg';

/* ──────────────────────────────────────────────────────────────
   DOCUMENT 1 — Claim Form
   ────────────────────────────────────────────────────────────── */
function buildClaimForm() {
  const pdf = new PdfBuilder();
  const H = 842, W = 595;
  const ops = [];

  // Cream background
  ops.push('0.98 0.96 0.90 rg');
  ops.push(`0 0 ${W} ${H} re f`);
  ops.push(black);

  // Outer border
  ops.push('0.6 0.5 0.4 rg');
  ops.push(`30 30 ${W-60} ${H-60} re S`);
  ops.push(black);

  // Header band
  ops.push('0.20 0.35 0.55 rg');
  ops.push(`30 ${H-90} ${W-60} 60 re f`);
  ops.push('1 1 1 rg');
  ops.push(text(180, H-68, 'MEDICAL INSURANCE CLAIM FORM', 'F2', 15));
  ops.push(text(240, H-85, '(Patient Submission Copy)', 'F3', 9));
  ops.push(black);

  // Ref + Date line
  ops.push('0.45 0.45 0.45 rg');
  ops.push(text(45, H-110, 'Claim Ref. No: _______________', 'F3', 9));
  ops.push(text(360, H-110, 'Date:  15 / 04 / 2026', 'F3', 9));
  ops.push(black);

  ops.push(line(45, H-115, W-45, H-115));

  // Section: Patient Info
  ops.push('0.15 0.30 0.50 rg');
  ops.push(text(45, H-135, 'SECTION A — PATIENT INFORMATION', 'F2', 10));
  ops.push(black);
  ops.push(line(45, H-140, W-45, H-140, 0.3));

  const fields1 = [
    ['Patient Name :', 'John  Doe', 45, H-162],
    ['Age :', '32  Yrs', 45, H-185],
    ['Gender :', 'Male', 200, H-185],
    ['Policy Number :', 'POL-778899', 45, H-208],
  ];

  for (const [label, val, x, y] of fields1) {
    ops.push('0.35 0.35 0.35 rg');
    ops.push(text(x, y, label, 'F3', 10));
    ops.push(black);
    // Slight slant simulation with different y offset
    ops.push(text(x + 105, y + 1, val, 'F1', 11));
    ops.push(line(x + 105, y - 4, x + 280, y - 4, 0.3));
  }

  // Section: Hospital
  ops.push('0.15 0.30 0.50 rg');
  ops.push(text(45, H-235, 'SECTION B — HOSPITAL / TREATMENT DETAILS', 'F2', 10));
  ops.push(black);
  ops.push(line(45, H-240, W-45, H-240, 0.3));

  const fields2 = [
    ['Hospital Name :', 'City Care  Hospital', 45, H-262],
    ['Hospital Address :', '123, Healthcare Ave, Medical District', 45, H-285],
    ['Treating Doctor :', 'Dr. R.  Sharma', 45, H-308],
    ['Ward / Room No. :', '204-B', 45, H-331],
    ['Admission Date :', '12 - 04 - 2026', 45, H-354],
    ['Discharge Date :', '15 - 04 - 2026', 320, H-354],
    ['No. of Days :', '3  Days', 45, H-377],
  ];

  for (const [label, val, x, y] of fields2) {
    ops.push('0.35 0.35 0.35 rg');
    ops.push(text(x, y, label, 'F3', 10));
    ops.push(black);
    ops.push(text(x + 118, y + 1, val, 'F1', 11));
    ops.push(line(x + 118, y - 4, x + 300, y - 4, 0.3));
  }

  // Section: Diagnosis
  ops.push('0.15 0.30 0.50 rg');
  ops.push(text(45, H-405, 'SECTION C — DIAGNOSIS & CLAIM', 'F2', 10));
  ops.push(black);
  ops.push(line(45, H-410, W-45, H-410, 0.3));

  ops.push('0.35 0.35 0.35 rg');
  ops.push(text(45, H-432, 'Primary Diagnosis :', 'F3', 10));
  ops.push(black);
  ops.push(text(163, H-431, 'Viral  Fever', 'F1', 12));
  ops.push(line(163, H-436, 420, H-436, 0.3));

  ops.push('0.35 0.35 0.35 rg');
  ops.push(text(45, H-458, 'ICD Code (if known) :', 'F3', 10));
  ops.push(black);
  ops.push(text(170, H-457, 'B34.9', 'F1', 11));

  // Claimed amount — highlighted box
  ops.push('0.90 0.95 0.85 rg');
  ops.push(`44 ${H-510} 510 50 re f`);
  ops.push(black);
  ops.push('0.15 0.30 0.50 rg');
  ops.push(text(55, H-480, 'TOTAL CLAIMED AMOUNT :', 'F2', 12));
  ops.push('0.10 0.50 0.20 rg');
  ops.push(text(270, H-480, 'USD  1,200.00', 'F2', 14));
  ops.push(black);
  ops.push('0.35 0.35 0.35 rg');
  ops.push(text(55, H-500, '(In Words : One Thousand Two Hundred US Dollars Only)', 'F3', 9));
  ops.push(black);

  // Supporting docs
  ops.push('0.15 0.30 0.50 rg');
  ops.push(text(45, H-530, 'SECTION D — SUPPORTING DOCUMENTS', 'F2', 10));
  ops.push(black);
  ops.push(line(45, H-535, W-45, H-535, 0.3));

  const docs = ['[x] Hospital Bill / Invoice', '[x] Discharge Summary', '[x] Lab Reports', '[ ] Prescription Copy', '[ ] Other: ___________'];
  docs.forEach((d, i) => {
    ops.push(text(i % 2 === 0 ? 55 : 310, H - 555 - Math.floor(i / 2) * 20, d, 'F3', 10));
  });

  // Declaration box
  ops.push('0.95 0.92 0.85 rg');
  ops.push(`44 ${H-680} 510 80 re f`);
  ops.push(black);
  ops.push('0.35 0.35 0.35 rg');
  ops.push(text(55, H-612, 'DECLARATION', 'F2', 10));
  ops.push(text(55, H-628, 'I hereby certify that the information provided above is true, complete and', 'F3', 8.5));
  ops.push(text(55, H-641, 'correct to the best of my knowlege. I understand that any misrepresentation', 'F3', 8.5));
  ops.push(text(55, H-654, 'may result in forfieture of claim benifits and legal action.', 'F3', 8.5));
  ops.push(black);

  // Signature area
  ops.push(text(55, H-710, 'Patient / Claimant Signature :', 'F3', 10));
  // Signature scribble (bezier-like signature using lines)
  ops.push('0.10 0.10 0.60 rg');
  ops.push('1.5 w');
  ops.push('230 135 m 240 148 l 255 128 l 270 145 l 285 130 l 295 142 l 310 125 l S');
  ops.push('230 125 m 315 125 l S');
  ops.push(black);
  ops.push('0.5 w');
  ops.push(text(55, H-735, 'Date :  15 / 04 / 2026', 'F3', 10));
  ops.push(text(360, H-710, 'Authorized by :', 'F3', 10));
  ops.push(line(455, H-710, 540, H-710, 0.4));
  ops.push(text(360, H-730, 'Designation :', 'F3', 10));
  ops.push(line(443, H-730, 540, H-730, 0.4));

  // Footer
  ops.push(line(45, 55, W-45, 55, 0.3));
  ops.push('0.55 0.55 0.55 rg');
  ops.push(text(55, 42, 'For Office Use Only   |   Received By: _______________   |   Claim Status: PENDING', 'F3', 8));
  ops.push(black);

  pdf.addPage(ops);
  return pdf.build();
}

/* ──────────────────────────────────────────────────────────────
   DOCUMENT 2 — Hospital Bill
   ────────────────────────────────────────────────────────────── */
function buildHospitalBill() {
  const pdf = new PdfBuilder();
  const H = 842, W = 595;
  const ops = [];

  // White background
  ops.push('1 1 1 rg');
  ops.push(`0 0 ${W} ${H} re f`);
  ops.push(black);

  // Header
  ops.push('0.10 0.25 0.50 rg');
  ops.push(`0 ${H-100} ${W} 100 re f`);
  ops.push('1 1 1 rg');
  ops.push(text(160, H-38, 'CITY CARE HOSPITAL', 'F2', 20));
  ops.push(text(130, H-58, '123, Healthcare Avenue, Medical District', 'F3', 10));
  ops.push(text(145, H-74, 'Tel: +91-80-2234-5678   |   NABH Accredited', 'F3', 9));
  ops.push(text(230, H-91, 'Reg. No: KA-HC-20045', 'F3', 8));
  ops.push(black);

  // Title bar
  ops.push('0.85 0.90 0.95 rg');
  ops.push(`0 ${H-125} ${W} 25 re f`);
  ops.push(black);
  ops.push('0.10 0.25 0.50 rg');
  ops.push(text(195, H-115, 'PATIENT BILL SUMMARY', 'F2', 13));
  ops.push(black);

  // Bill meta
  ops.push(text(40, H-150, 'Bill No  :', 'F3', 10));
  ops.push(text(110, H-150, 'HOSP-4455', 'F2', 10));
  ops.push(text(360, H-150, 'Bill Date :', 'F3', 10));
  ops.push(text(430, H-150, '15-04-2026', 'F1', 10));

  ops.push(line(40, H-158, W-40, H-158, 0.3));

  // Patient info box
  ops.push('0.96 0.97 0.98 rg');
  ops.push(`38 ${H-240} ${W-76} 75 re f`);
  ops.push('0.80 0.82 0.85 rg');
  ops.push(`38 ${H-240} ${W-76} 75 re S`);
  ops.push(black);

  const pInfo = [
    ['Patient Name  :', 'John  Doe', 45, H-178],
    ['Age / Gender  :', '32 Yrs  /  Male', 45, H-196],
    ['Ward / Room   :', 'General  -  Room 204B', 45, H-214],
    ['Admission     :', '12-04-2026', 45, H-232],
    ['Discharge     :', '15-04-2026', 300, H-232],
  ];
  ops.push('0.30 0.30 0.30 rg');
  for (const [label, val, x, y] of pInfo) {
    ops.push(text(x, y, label, 'F3', 10));
    ops.push(black);
    ops.push(text(x + 115, y + 1, val, 'F1', 10));
    ops.push('0.30 0.30 0.30 rg');
  }
  ops.push(black);

  ops.push(text(45, H-250, 'Treating Doctor  :', 'F3', 10));
  ops.push(text(163, H-249, 'Dr. R.  Sharma', 'F1', 10));
  ops.push(text(360, H-250, 'Diagnosis :', 'F3', 10));
  ops.push(text(433, H-249, 'Viral  Fever', 'F1', 10));

  ops.push(line(40, H-262, W-40, H-262, 0.5));

  // Charges table header
  ops.push('0.10 0.25 0.50 rg');
  ops.push(`40 ${H-284} ${W-80} 20 re f`);
  ops.push('1 1 1 rg');
  ops.push(text(50, H-276, 'S.No', 'F2', 9));
  ops.push(text(100, H-276, 'Description of Charges', 'F2', 9));
  ops.push(text(370, H-276, 'Rate (USD)', 'F2', 9));
  ops.push(text(468, H-276, 'Amount', 'F2', 9));
  ops.push(black);

  // Table rows
  const rows = [
    ['1', 'Room Charges (General Ward, 3 nights)', '133.33/night', '400.00'],
    ['2', 'Medicines & Pharmacy Supplies', '—', '300.00'],
    ['3', 'Laboratory Tests (CBC, Typhidot, LFT)', '—', '200.00'],
    ['4', 'Doctor Consultation Fees', '100.00/visit', '300.00'],
  ];

  rows.forEach(([no, desc, rate, amt], i) => {
    const y = H - 300 - i * 28;
    if (i % 2 === 0) {
      ops.push('0.96 0.97 1.0 rg');
      ops.push(`40 ${y - 10} ${W - 80} 26 re f`);
      ops.push(black);
    }
    ops.push('0.80 0.82 0.85 rg');
    ops.push(`40 ${y - 10} ${W - 80} 26 re S`);
    ops.push(black);
    ops.push(text(50, y + 3, no, 'F3', 10));
    ops.push(text(100, y + 3, desc, 'F3', 10));
    ops.push(text(370, y + 3, rate, 'F3', 9));
    ops.push('0.10 0.10 0.10 rg');
    ops.push(text(470, y + 3, amt, 'F1', 10));
    ops.push(black);
  });

  // Subtotal area
  const subY = H - 420;
  ops.push(line(40, subY, W-40, subY, 0.3));
  ops.push(text(360, subY - 18, 'Sub Total  :', 'F3', 10));
  ops.push(text(470, subY - 18, '1,200.00', 'F1', 10));
  ops.push(text(360, subY - 36, 'Discount   :', 'F3', 10));
  ops.push(text(478, subY - 36, '0.00', 'F1', 10));
  ops.push(text(360, subY - 54, 'Tax (0%)   :', 'F3', 10));
  ops.push(text(478, subY - 54, '0.00', 'F1', 10));

  // Total box
  ops.push('0.10 0.40 0.20 rg');
  ops.push(`40 ${subY - 90} ${W-80} 28 re f`);
  ops.push('1 1 1 rg');
  ops.push(text(50, subY - 78, 'TOTAL AMOUNT PAYABLE :', 'F2', 12));
  ops.push('0.80 1.0 0.80 rg');
  ops.push(text(430, subY - 78, 'USD 1,200.00', 'F2', 13));
  ops.push(black);

  // In words
  ops.push('0.40 0.40 0.40 rg');
  ops.push(text(50, subY - 105, 'Amount in words : One Thousand Two Hundred US Dollars Only', 'F3', 9));
  ops.push(black);

  // Payment info
  ops.push(line(40, subY - 115, W-40, subY - 115, 0.3));
  ops.push(text(50, subY - 130, 'Payment Mode  :', 'F3', 10));
  ops.push(text(170, subY - 130, 'Insurance Claim (TPA)', 'F1', 10));
  ops.push(text(360, subY - 130, 'Policy No :', 'F3', 10));
  ops.push(text(428, subY - 130, 'POL-778899', 'F1', 10));
  ops.push(text(50, subY - 148, 'Insurance Co. :', 'F3', 10));
  ops.push(text(160, subY - 148, 'TPA Insurance Services Ltd.', 'F1', 10));

  // PAID stamp (tilted via rotation)
  ops.push('q');
  ops.push('1 0 0 1 440 320 cm');
  ops.push(`cos(-0.15) sin(-0.15) -sin(-0.15) cos(-0.15) 0 0 cm`);
  ops.push('0.85 0.10 0.10 rg');
  ops.push('2 w');
  ops.push('-35 -18 70 36 re S');
  ops.push(text(-28, -8, 'P  A  I  D', 'F2', 16));
  ops.push('Q');
  ops.push(black);

  // Signature section
  ops.push(line(40, 180, W-40, 180, 0.3));
  ops.push(text(50, 165, 'Prepared By :', 'F3', 10));
  ops.push(line(130, 165, 250, 165, 0.4));
  ops.push(text(280, 165, 'Verified By :', 'F3', 10));
  ops.push(line(360, 165, 480, 165, 0.4));
  ops.push(text(50, 148, 'Designation :', 'F3', 10));
  ops.push(line(133, 148, 250, 148, 0.4));
  ops.push(text(280, 148, 'Designation :', 'F3', 10));
  ops.push(line(362, 148, 480, 148, 0.4));

  // Footer
  ops.push('0.10 0.25 0.50 rg');
  ops.push(`0 0 ${W} 40 re f`);
  ops.push('0.85 0.90 0.95 rg');
  ops.push(text(80, 16, 'This is a system generated bill.  Please verify all amounts before processing.', 'F3', 8));
  ops.push(text(220, 5, 'City Care Hospital — All rights reserved.', 'F3', 7));
  ops.push(black);

  pdf.addPage(ops);
  return pdf.build();
}

/* ── Write files ── */
const claimPath = path.join(OUT_DIR, 'claim_form.pdf');
const billPath  = path.join(OUT_DIR, 'hospital_bill.pdf');

fs.writeFileSync(claimPath,  Buffer.from(buildClaimForm(),   'binary'));
fs.writeFileSync(billPath,   Buffer.from(buildHospitalBill(), 'binary'));

console.log('✅ PDFs generated successfully!');
console.log('📄 Claim Form  :', claimPath);
console.log('📄 Hospital Bill:', billPath);
console.log('📂 Folder      :', OUT_DIR);
