from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import letter
import json
import os

# Helper to write JSON files
def write_json(filename, data):
    with open(filename, 'w') as f:
        json.dump(data, f, indent=2)

# Valid JSON
valid_json = {
  "policyNumber": "POL-US-9876-1234",
  "patientName": "Michael T. Anderson",
  "patientAge": 45,
  "patientGender": "Male",
  "hospitalName": "City Care General Hospital",
  "hospitalAddress": "1200 Healthcare Blvd, Metro City, NY 10001",
  "admissionDate": "2026-04-10",
  "dischargeDate": "2026-04-15",
  "doctorName": "Dr. Sarah Jenkins",
  "roomNumber": "402-B",
  "claimType": "Reimbursement",
  "diagnosis": "Acute Appendicitis - Post Appendectomy",
  "claimedAmount": 4800.00,
  "totalBillAmount": 4800.00,
  "currency": "USD",
  "documents": [
    { "type": "CLAIM_FORM", "fileName": "claim_form_valid.pdf" },
    { "type": "HOSPITAL_BILL", "fileName": "hospital_bill_valid.pdf" }
  ]
}

# Fraud JSON
fraud_json = {
  "policyNumber": "POL-XX-5555-9999",
  "patientName": "Sarah J. Connor",
  "patientAge": 32,
  "patientGender": "Female",
  "hospitalName": "Metro Health Center",
  "hospitalAddress": "789 Medical Parkway, Austin, TX 78701",
  "admissionDate": "2026-04-12",
  "dischargeDate": "2026-04-20",
  "doctorName": "Dr. Alan Grant",
  "roomNumber": "105",
  "claimType": "Reimbursement",
  "diagnosis": "Viral Fever and Exhaustion",
  "claimedAmount": 7500.00,
  "totalBillAmount": 7500.00,
  "currency": "USD",
  "documents": [
    { "type": "CLAIM_FORM", "fileName": "claim_form_fraud.pdf" },
    { "type": "HOSPITAL_BILL", "fileName": "hospital_bill_fraud.pdf" }
  ]
}

write_json("valid_claim.json", valid_json)
write_json("fraud_claim.json", fraud_json)

# Create Valid Claim Form PDF
def create_claim_form(filename, data, is_fraud):
    c = canvas.Canvas(filename, pagesize=letter)
    
    # Skew slightly for realism
    c.translate(5, 5)
    c.rotate(0.5 if not is_fraud else -0.8)
    
    # Header
    c.setFont("Helvetica-Bold", 16)
    c.drawString(50, 750, "HEALTH INSURANCE CLAIM FORM - PART A")
    c.setFont("Helvetica", 12)
    c.drawString(50, 735, "---------------------------------------------------------")
    
    c.setFont("Helvetica", 11)
    # Using simple text since custom font might not be installed, we use a slanted built-in font for handwriting-ish feel
    c.setFont("Helvetica-Oblique", 13)
    
    if not is_fraud:
        c.drawString(50, 710, "1. Policy Number: POL-US-9876-1234")
        c.drawString(50, 690, "2. Patient Name: Michael T. Anderson")
        c.drawString(50, 670, "3. Age: 45      Gender: Male")
        c.drawString(50, 650, "4. Address: 452 West Avenue, Apt 4B, Metro City, NY 10012")
        
        c.setFont("Helvetica-Bold", 12)
        c.drawString(50, 610, "HOSPITALIZATION DETAILS")
        c.setFont("Helvetica", 12)
        c.drawString(50, 595, "---------------------------------------------------------")
        c.setFont("Helvetica-Oblique", 13)
        
        c.drawString(50, 570, "5. Name of Hospital: City Care General Hospital")
        c.drawString(50, 550, "6. Date of Admission: 10-Apr-2026")
        c.drawString(50, 530, "7. Date of Discharge: 15-Apr-2026")
        c.drawString(50, 510, "8. Diagnosis: Acute Appendicitis")
        c.drawString(50, 490, "   (Surgery went well. No complications to my best knowlege)") # subtle spelling mistake
        
        c.setFont("Helvetica-Bold", 12)
        c.drawString(50, 450, "FINANCIAL DETAILS")
        c.setFont("Helvetica", 12)
        c.drawString(50, 435, "---------------------------------------------------------")
        c.setFont("Helvetica-Oblique", 13)
        
        c.drawString(50, 410, "9. Total Claimed Amount: $ 4800.00 USD")
        c.drawString(50, 390, "10. Claim Type: Reimbursement")
        
        c.setFont("Helvetica-Bold", 12)
        c.drawString(50, 350, "DECLARATION")
        c.setFont("Helvetica", 12)
        c.drawString(50, 335, "---------------------------------------------------------")
        c.setFont("Helvetica-Oblique", 12)
        
        c.drawString(50, 310, "I hereby declare that the information furnished in this")
        c.drawString(50, 290, "claim form is true and correct to the best of my knowledge")
        c.drawString(50, 270, "and belief. If I have made any false or untrue statement,")
        c.drawString(50, 250, "my right to claim reimbursement shall be forfeited.")
        
        c.drawString(50, 200, "Date: 18-Apr-2026")
        c.setFont("Times-Italic", 20)
        c.drawString(50, 160, "Signature: M. Anderson")
        
    else:
        # Fraud Case
        c.drawString(50, 710, "1. Policy Number: POL-XX-5555-9999")
        c.drawString(50, 690, "2. Patient Name: Sarah O. Connor") # Mismatch
        c.drawString(50, 670, "3. Age: 32      Gender: Female")
        c.drawString(50, 650, "4. Address: 12 Elm Street, Austin, TX")
        
        c.setFont("Helvetica-Bold", 12)
        c.drawString(50, 610, "HOSPITALIZATION DETAILS")
        c.setFont("Helvetica", 12)
        c.drawString(50, 595, "---------------------------------------------------------")
        c.setFont("Helvetica-Oblique", 13)
        
        c.drawString(50, 570, "5. Name of Hospital: Metro Health Center")
        c.drawString(50, 550, "6. Date of Admission: 12-Apr-2026")
        c.drawString(50, 530, "7. Date of Discharge:     -Apr-2026 (xxxx)") # Smeared/Scribbled out
        c.drawString(50, 510, "8. Diagnosis: Severe viral fever")
        
        c.setFont("Helvetica-Bold", 12)
        c.drawString(50, 450, "FINANCIAL DETAILS")
        c.setFont("Helvetica", 12)
        c.drawString(50, 435, "---------------------------------------------------------")
        c.setFont("Helvetica-Oblique", 13)
        
        c.drawString(50, 410, "9. Total Claimed Amount: $ 7500.00 USD") # Amount mismatch with bill components
        c.drawString(50, 390, "10. Claim Type: Re-imbursement")
        
        c.setFont("Helvetica-Bold", 12)
        c.drawString(50, 350, "DECLARATION")
        c.setFont("Helvetica", 12)
        c.drawString(50, 335, "---------------------------------------------------------")
        c.setFont("Helvetica-Oblique", 12)
        
        c.drawString(50, 310, "I hereby declare that the information furnished in this")
        c.drawString(50, 290, "claim form is true and correct to the best of my knowledge")
        c.drawString(50, 270, "and belief. If I have made any false or untrue statement,")
        c.drawString(50, 250, "my right to claim reimbursement shall be forfeited.")
        
        c.drawString(50, 200, "Date: 22-Apr-2026")
        c.setFont("Times-Italic", 20)
        c.drawString(50, 160, "Signature: S. Connor")
    
    c.save()

# Create Hospital Bill PDF
def create_hospital_bill(filename, data, is_fraud):
    c = canvas.Canvas(filename, pagesize=letter)
    c.setFont("Courier-Bold", 14)
    
    if not is_fraud:
        c.drawString(180, 750, "CITY CARE GENERAL HOSPITAL")
        c.setFont("Courier", 10)
        c.drawString(160, 735, "1200 Healthcare Blvd, Metro City, NY 10001")
        c.drawString(180, 720, "Phone: 555-0198 | Tax ID: 88-7654321")
        c.drawString(50, 700, "=======================================================================")
        
        c.setFont("Courier-Bold", 12)
        c.drawString(50, 680, "FINAL INPATIENT BILL")
        
        c.setFont("Courier", 11)
        c.drawString(50, 650, "Bill No: INV-2026-08992")
        c.drawString(350, 650, "Date: April 15, 2026")
        c.drawString(50, 630, "Patient Name: Michael T. Anderson")
        c.drawString(50, 610, "Admitted: 2026-04-10")
        c.drawString(350, 610, "Discharged: 2026-04-15")
        c.drawString(50, 590, "Attending Physician: Dr. Sarah Jenkins")
        c.drawString(350, 590, "Room: 402-B")
        
        c.drawString(50, 550, "-----------------------------------------------------------------------")
        c.setFont("Courier-Bold", 11)
        c.drawString(50, 530, "DESCRIPTION")
        c.drawString(450, 530, "AMOUNT (USD)")
        c.setFont("Courier", 11)
        c.drawString(50, 510, "-----------------------------------------------------------------------")
        
        c.drawString(50, 490, "Room Charges (5 Days @ $400/day)")
        c.drawString(450, 490, "$ 2000.00")
        c.drawString(50, 470, "Doctor Fees / Surgical Fees")
        c.drawString(450, 470, "$ 1500.00")
        c.drawString(50, 450, "Medicines & Pharmacy")
        c.drawString(450, 450, "$  800.00")
        c.drawString(50, 430, "Lab Tests & Pathology")
        c.drawString(450, 430, "$  500.00")
        
        c.drawString(50, 400, "-----------------------------------------------------------------------")
        c.setFont("Courier-Bold", 11)
        c.drawString(50, 380, "SUBTOTAL")
        c.drawString(450, 380, "$ 4800.00")
        c.drawString(50, 360, "TAX")
        c.drawString(450, 360, "$    0.00")
        c.drawString(50, 340, "-----------------------------------------------------------------------")
        c.drawString(50, 320, "TOTAL AMOUNT DUE")
        c.drawString(450, 320, "$ 4800.00")
        c.drawString(50, 300, "-----------------------------------------------------------------------")
        
        # PAID STAMP
        c.setFillColorRGB(0.8, 0.1, 0.1, alpha=0.6)
        c.setFont("Helvetica-Bold", 24)
        c.rotate(15)
        c.drawString(200, 100, "P A I D   I N   F U L L")
        c.rotate(-15)
        c.setFillColorRGB(0, 0, 0)
        
        c.setFont("Courier", 10)
        c.drawString(200, 200, "Date: April 15, 2026")
        c.drawString(200, 185, "Cashier: Desk #4")
        
        c.drawString(180, 120, "Thank you for choosing City Care General Hospital.")
        
    else:
        # Fraud Case
        c.drawString(180, 750, "METRO HEALTH CENTER")
        c.setFont("Courier", 10)
        c.drawString(160, 735, "789 Medical Parkway, Austin, TX 78701")
        c.drawString(180, 720, "Phone: 555-0999 | Tax ID: 12-3456789")
        c.drawString(50, 700, "=======================================================================")
        
        c.setFont("Courier-Bold", 12)
        c.drawString(50, 680, "INPATIENT INVOICE")
        
        c.setFont("Courier", 11)
        c.drawString(50, 650, "Bill No: INV-2026-00344")
        c.drawString(350, 650, "Date: April 18, 2026") # Earlier than claim discharge date
        c.drawString(50, 630, "Patient Name: Sara Connor") # Name mismatch
        c.drawString(50, 610, "Admitted: 2026-04-12")
        c.drawString(350, 610, "Discharged: 2026-04-18") # Earlier than claim
        c.drawString(50, 590, "Attending Physician: Dr. Alan Grant")
        c.drawString(350, 590, "Room: 105")
        
        c.drawString(50, 550, "-----------------------------------------------------------------------")
        c.setFont("Courier-Bold", 11)
        c.drawString(50, 530, "DESCRIPTION")
        c.drawString(450, 530, "AMOUNT (USD)")
        c.setFont("Courier", 11)
        c.drawString(50, 510, "-----------------------------------------------------------------------")
        
        # Fraudulent values
        c.drawString(50, 490, "Room Charges (6 Days @ $300/day)")
        c.drawString(450, 490, "$ 1800.00")
        c.drawString(50, 470, "Doctor Consultation Fees")
        c.drawString(450, 470, "$ 1500.00")
        c.drawString(50, 450, "Medicines & Pharmacy")
        c.drawString(450, 450, "$ 1800.00")
        c.drawString(50, 430, "Medicines & Pharmacy") # Duplicate
        c.drawString(450, 430, "$ 1800.00")
        c.drawString(50, 410, "Lab Tests")
        c.drawString(450, 410, "$  500.00")
        
        c.drawString(50, 380, "-----------------------------------------------------------------------")
        c.setFont("Courier-Bold", 11)
        # 1800 + 1500 + 1800 + 1800 + 500 = 7400
        c.drawString(50, 360, "SUBTOTAL")
        c.drawString(450, 360, "$ 7400.00")
        c.drawString(50, 340, "TAX")
        c.drawString(450, 340, "$  100.00")
        c.drawString(50, 320, "-----------------------------------------------------------------------")
        # Total is claimed to be 7500, mathematically this adds up but 7400+100=7500 which is forced.
        c.drawString(50, 300, "TOTAL AMOUNT DUE")
        c.drawString(450, 300, "$ 7500.00")
        c.drawString(50, 280, "-----------------------------------------------------------------------")
        
        # PAID STAMP
        c.setFillColorRGB(0.8, 0.1, 0.1, alpha=0.6)
        c.setFont("Helvetica-Bold", 24)
        c.rotate(-5)
        c.drawString(200, 200, "P A I D")
        c.rotate(5)
        c.setFillColorRGB(0, 0, 0)
        
        c.setFont("Courier", 10)
        c.drawString(200, 160, "Date: April 18, 2026")
        
        c.drawString(180, 120, "Thank you for choosing Metro Health Center.")
        
    c.save()

create_claim_form("claim_form_valid.pdf", valid_json, False)
create_hospital_bill("hospital_bill_valid.pdf", valid_json, False)

create_claim_form("claim_form_fraud.pdf", fraud_json, True)
create_hospital_bill("hospital_bill_fraud.pdf", fraud_json, True)
