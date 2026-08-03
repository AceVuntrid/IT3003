# University Asset Management and Reservation System

## Complete Software Project Specification for a Coding Agent

**Project Type:** Full-stack web application  
**Backend:** Java 21 with Spring Boot  
**Frontend:** React with TypeScript  
**Database:** PostgreSQL recommended; MySQL supported  
**Build Tool:** Maven  
**API Style:** REST JSON API  
**Authentication:** Spring Security with JWT access and refresh tokens  
**Initial Deployment Scope:** University Science Faculty  
**Future Scope:** Expandable to all university faculties and departments

---

# 1. Project Overview

The University Asset Management and Reservation System is a centralized web application used to register, locate, reserve, allocate, maintain, audit, and report on university assets.

The system must manage both:

1. **Fixed assets**
   - Laboratory equipment
   - Computers
   - Microscopes
   - Projectors
   - Furniture
   - Vehicles
   - Measuring instruments
   - Machines

2. **Consumable assets**
   - Chemicals
   - Glassware
   - Lab supplies
   - Printer cartridges
   - Stationery
   - Disposable safety equipment

The system must show what assets are available, where they are located, who is responsible for them, their condition, value, quantity, usage history, reservation history, maintenance records, and related payments or charges.

The first version should focus on science faculties and laboratories, but the architecture must support future use across the entire university.

---

# 2. Main Objectives

The application must:

- Maintain one central record of all university assets.
- Record fixed assets and consumable items separately.
- Track the exact location of every asset.
- Track asset quantity, condition, value, and availability.
- Allow authorized users to reserve equipment, rooms, and facilities.
- Allow laboratory staff to issue consumable items.
- Record asset usage, returns, transfers, losses, and damage.
- Record maintenance and calibration details.
- Record payments, fees, deposits, penalties, and departmental charges.
- Store supporting documents such as invoices, receipts, warranties, and manuals.
- Provide role-based access.
- Keep a complete audit history of important actions.
- Generate operational and management reports.

---

# 3. Recommended Technology Stack

## 3.1 Backend

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Validation
- Hibernate
- JWT authentication
- MapStruct or manual DTO mapping
- Lombok optional
- Flyway for database migrations
- OpenAPI / Swagger documentation
- Maven

## 3.2 Frontend

Recommended choice:

- React 19+
- TypeScript
- Vite
- React Router
- TanStack Query
- React Hook Form
- Zod validation
- Material UI or Ant Design
- Axios
- Recharts for dashboards

Alternative frontends:

- Angular
- Vue

The coding agent should use React unless specifically changed.

## 3.3 Database

Recommended:

- PostgreSQL

Supported alternative:

- MySQL

## 3.4 Infrastructure

- Docker
- Docker Compose
- Nginx reverse proxy
- Local file storage during development
- Amazon S3-compatible storage for production documents
- GitHub Actions for CI

---

# 4. System Users and Roles

## 4.1 Super Administrator

Can:

- Configure the entire system
- Create faculties and departments
- Create locations and laboratories
- Create and manage users
- Assign roles and permissions
- View all assets
- Approve or reject high-value changes
- View all reports
- Manage system settings
- View audit logs

## 4.2 Asset Administrator

Can:

- Add assets
- Edit assets
- Archive assets
- Transfer assets
- Add asset documents
- Manage categories and suppliers
- Record purchases and warranties
- Run asset reports

## 4.3 Faculty Administrator

Can:

- Manage assets belonging to a faculty
- View faculty reports
- Approve reservations
- Manage faculty locations
- Manage department custodians

## 4.4 Laboratory Manager

Can:

- Manage laboratory equipment
- Approve equipment reservations
- Issue and receive equipment
- Issue consumables
- Record damage and maintenance requests
- View laboratory stock

## 4.5 Storekeeper

Can:

- Receive consumable inventory
- Issue consumable items
- Adjust stock with approval
- View low-stock items
- Record batch numbers and expiry dates

## 4.6 Maintenance Officer

Can:

- View assigned maintenance jobs
- Update maintenance status
- Record repair details and cost
- Upload service documents
- Mark assets as returned to service

## 4.7 Finance Officer

Can:

- View purchase and payment details
- Record charges, deposits, refunds, and penalties
- Review financial reports
- Approve selected financial transactions

## 4.8 Lecturer or Researcher

Can:

- Search assets
- Check availability
- Request reservations
- View personal reservations
- Report damage or faults
- Request consumable items

## 4.9 Student

Can:

- Search permitted assets
- Request reservations where allowed
- View personal reservations
- Check items issued to them
- Report a fault

## 4.10 Auditor or Read-Only User

Can:

- View permitted records
- View reports
- View audit information
- Cannot create, update, approve, or delete records

---

# 5. High-Level Navigation Map

The main application navigation should contain:

1. Dashboard
2. Assets
3. Consumables
4. Reservations
5. Check-Out and Returns
6. Locations
7. Maintenance
8. Transfers
9. Purchases and Suppliers
10. Payments and Charges
11. Reports
12. Users and Roles
13. Audit Log
14. Notifications
15. Settings
16. My Profile
17. Help and Documentation

Navigation items must be displayed according to permissions.

---

# 6. Authentication Pages

## 6.1 Login Page

### Fields

- Email address or username
- Password
- Remember me checkbox

### Buttons

- Sign In
- Forgot Password

### Functions

- Validate required fields
- Show invalid-credentials error
- Lock account temporarily after repeated failed attempts
- Redirect authenticated users to the dashboard
- Support password visibility toggle

## 6.2 Forgot Password Page

### Fields

- Email address

### Buttons

- Send Reset Link
- Back to Login

## 6.3 Reset Password Page

### Fields

- New password
- Confirm password

### Validation

- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- At least one special character
- Passwords must match

### Buttons

- Reset Password

## 6.4 First Login Page

Users created by an administrator must be required to:

- Change temporary password
- Confirm contact details
- Accept terms of use

## 6.5 Access Denied Page

Display when the user attempts to access an unauthorized page.

---

# 7. Dashboard

The dashboard must change according to the user's role.

## 7.1 Dashboard Summary Cards

- Total fixed assets
- Total asset value
- Available assets
- Reserved assets
- Checked-out assets
- Assets under maintenance
- Damaged assets
- Lost assets
- Low-stock consumables
- Expiring consumables
- Pending reservation approvals
- Overdue returns
- Maintenance jobs due

## 7.2 Dashboard Charts

- Assets by category
- Assets by faculty
- Assets by condition
- Monthly reservation count
- Monthly maintenance cost
- Consumable usage trend
- Asset acquisition value by year

## 7.3 Dashboard Tables

- Recent reservations
- Overdue items
- Low-stock items
- Upcoming maintenance
- Recent asset activity
- Pending approvals

## 7.4 Dashboard Actions

- Add Asset
- Add Consumable
- Create Reservation
- Check Out Asset
- Record Return
- Create Maintenance Request
- Generate Report

Buttons must appear only when the user has permission.

---

# 8. Asset Management Module

## 8.1 Asset List Page

### Table Columns

- Asset code
- Asset name
- Category
- Faculty
- Department
- Location
- Asset type
- Serial number
- Condition
- Status
- Quantity
- Custodian
- Purchase value
- Current book value
- Next maintenance date
- Actions

### Filters

- Search by name, code, serial number, barcode, or QR code
- Faculty
- Department
- Location
- Category
- Asset type
- Status
- Condition
- Custodian
- Purchase date range
- Maintenance due status

### Buttons

- Add Asset
- Import Assets
- Export CSV
- Export Excel
- Print Labels
- Bulk Update
- Advanced Filters

### Row Actions

- View
- Edit
- Duplicate
- Transfer
- Reserve
- Check Out
- Send for Maintenance
- Mark Damaged
- Mark Lost
- Print QR Code
- Archive

Permanent deletion should normally not be available. Assets should be archived to preserve history.

## 8.2 Add Asset Page

The page should be divided into sections.

### Section A: Basic Information

Fields:

- Asset name
- Asset code
- Asset type
  - Fixed asset
  - Consumable
  - Facility
  - Room
  - Vehicle
- Category
- Subcategory
- Description
- Brand
- Model
- Manufacturer
- Serial number
- Barcode
- QR code value
- Tags

### Section B: Ownership and Location

Fields:

- University
- Faculty
- Department
- Building
- Floor
- Room
- Laboratory
- Storage area
- Exact location notes
- Custodian
- Responsible staff member

### Section C: Financial Information

Fields:

- Purchase price
- Currency
- Purchase date
- Supplier
- Purchase order number
- Invoice number
- Funding source
- Grant or project code
- Depreciation method
- Useful life in years
- Salvage value
- Current book value

### Section D: Asset Condition and Availability

Fields:

- Initial condition
- Current condition
- Status
- Availability status
- Quantity
- Minimum available quantity
- Can be reserved
- Reservation requires approval
- Maximum reservation duration
- Can be taken outside campus
- Deposit required
- Deposit amount

### Section E: Warranty and Maintenance

Fields:

- Warranty start date
- Warranty end date
- Warranty provider
- Service interval
- Last service date
- Next service date
- Calibration required
- Calibration interval
- Last calibration date
- Next calibration date

### Section F: Documents

Uploads:

- Invoice
- Purchase order
- Warranty document
- User manual
- Asset image
- Compliance certificate
- Calibration certificate
- Other supporting files

### Buttons

- Save Asset
- Save and Add Another
- Save as Draft
- Cancel

## 8.3 Asset Validation Rules

- Asset name is required.
- Asset code must be unique.
- Serial number should be unique when provided.
- Purchase price cannot be negative.
- Quantity must be greater than zero.
- Warranty end date cannot be before warranty start date.
- Next service date cannot be before last service date.
- Deposit amount is required when deposit required is enabled.
- Faculty, department, and location must follow the correct hierarchy.

## 8.4 Asset Details Page

### Header

- Asset image
- Asset name
- Asset code
- Status badge
- Condition badge
- Current location
- Custodian

### Tabs

1. Overview
2. Reservations
3. Check-Out History
4. Maintenance
5. Transfers
6. Financial Information
7. Documents
8. Activity Log

### Header Actions

- Edit
- Reserve
- Check Out
- Transfer
- Maintenance Request
- Print QR Label
- Archive

## 8.5 Edit Asset Page

Same fields as Add Asset, but changes to sensitive values must be logged.

Sensitive changes include:

- Asset code
- Serial number
- Purchase value
- Ownership
- Faculty
- Department
- Current location
- Custodian
- Status
- Condition

---

# 9. Consumable Inventory Module

Consumables must use quantity-based stock control.

## 9.1 Consumable List Page

### Columns

- Item code
- Item name
- Category
- Faculty
- Store or laboratory
- Unit of measure
- Current quantity
- Reserved quantity
- Available quantity
- Reorder level
- Expiry status
- Batch count
- Status
- Actions

### Filters

- Category
- Faculty
- Department
- Store
- Low stock
- Out of stock
- Expiring soon
- Expired
- Supplier

### Buttons

- Add Consumable
- Receive Stock
- Issue Stock
- Stock Adjustment
- Stock Transfer
- Import
- Export

## 9.2 Add Consumable Page

### Fields

- Item name
- Item code
- Category
- Description
- Brand
- Manufacturer
- Unit of measure
- Faculty
- Department
- Store location
- Current quantity
- Reorder level
- Maximum stock level
- Unit cost
- Supplier
- Batch number
- Manufacture date
- Expiry date
- Hazardous item checkbox
- Chemical classification
- Safety data sheet upload
- Storage instructions
- Disposal instructions

### Buttons

- Save
- Save and Add Another
- Cancel

## 9.3 Receive Stock Page

### Fields

- Consumable item
- Supplier
- Purchase order number
- Invoice number
- Batch number
- Quantity received
- Unit cost
- Manufacture date
- Expiry date
- Received date
- Received by
- Storage location
- Notes
- Attach receipt or invoice

### Buttons

- Confirm Receipt
- Save as Draft
- Cancel

## 9.4 Issue Stock Page

### Fields

- Consumable item
- Available quantity
- Quantity requested
- Quantity issued
- Unit of measure
- Issued to user
- Faculty
- Department
- Course or project
- Purpose
- Issued by
- Issue date
- Chargeable checkbox
- Charge amount
- Notes

### Buttons

- Issue Stock
- Cancel

## 9.5 Stock Adjustment Page

### Fields

- Item
- Current quantity
- Adjustment type
  - Increase
  - Decrease
- Quantity
- Reason
  - Stock count correction
  - Damage
  - Expiry
  - Disposal
  - Data correction
  - Other
- Approval reference
- Notes
- Attachment

### Buttons

- Submit Adjustment
- Cancel

All stock adjustments must be logged.

---

# 10. Reservation Module

Reservations may apply to equipment, rooms, laboratories, facilities, or vehicles.

## 10.1 Reservation List Page

### Columns

- Reservation number
- Asset or facility
- Requested by
- Start date and time
- End date and time
- Purpose
- Status
- Approval status
- Payment status
- Check-out status
- Actions

### Filters

- Date range
- Reservation status
- Approval status
- Asset
- Location
- Faculty
- Requested by
- Overdue status

### Buttons

- Create Reservation
- Calendar View
- List View
- Export

## 10.2 Reservation Calendar Page

Views:

- Day
- Week
- Month
- Resource timeline

Functions:

- Display availability
- Show conflicts
- Drag to create reservation
- Click reservation to view details
- Filter by asset, room, or laboratory

## 10.3 Create Reservation Page

### Fields

- Reservation type
- Asset, room, laboratory, or facility
- Requested by
- Faculty
- Department
- Purpose
- Course, project, or research name
- Start date
- Start time
- End date
- End time
- Number of participants
- Required accessories
- Special setup requirements
- External use requested
- Payment or deposit required
- Notes
- Supporting document

### Availability Panel

Must display:

- Available quantity
- Existing reservations
- Maintenance blocks
- Restricted dates
- Alternative assets or times

### Buttons

- Check Availability
- Submit Request
- Save Draft
- Cancel

## 10.4 Reservation Approval Page

### Information

- Request details
- Asset availability
- User history
- Existing conflicts
- Deposit or payment requirement
- Attachments

### Fields

- Approval decision
- Approved quantity
- Conditions
- Approval notes

### Buttons

- Approve
- Reject
- Request Changes
- Cancel

## 10.5 Reservation Statuses

- Draft
- Submitted
- Pending approval
- Approved
- Rejected
- Cancelled
- Ready for collection
- Checked out
- Completed
- Overdue
- No-show

## 10.6 Reservation Rules

- Reservations cannot overlap when capacity is unavailable.
- Assets under maintenance cannot be reserved.
- Archived, damaged, or lost assets cannot be reserved.
- Approval may be required according to asset configuration.
- Users may be blocked after repeated overdue returns.
- Reservation duration must not exceed the configured limit.
- External use may require special approval.
- Deposit payment may be required before check-out.

---

# 11. Check-Out and Return Module

## 11.1 Check-Out Page

### Fields

- Reservation number
- User
- Asset
- Quantity
- Asset condition before issue
- Accessories included
- Check-out date and time
- Expected return date and time
- Deposit paid
- Issued by
- Notes
- User acknowledgement checkbox

### Buttons

- Confirm Check-Out
- Print Issue Slip
- Cancel

### Functions

- Scan QR or barcode
- Confirm approved reservation
- Confirm deposit status
- Record current condition
- Update asset status to checked out

## 11.2 Return Page

### Fields

- Reservation or checkout number
- Asset
- User
- Actual return date and time
- Returned quantity
- Condition on return
- Missing accessories
- Damage detected
- Damage description
- Late return duration
- Penalty amount
- Deposit refund amount
- Received by
- Notes
- Upload photos

### Buttons

- Complete Return
- Send to Maintenance
- Record Damage Charge
- Print Return Receipt
- Cancel

## 11.3 Overdue Management Page

### Columns

- User
- Asset
- Due date
- Days overdue
- Contact information
- Penalty amount
- Reminder count
- Status
- Actions

### Actions

- Send Reminder
- Record Contact Attempt
- Extend Return Date
- Add Penalty
- Escalate
- Mark Returned

---

# 12. Location Management Module

Location hierarchy:

University → Campus → Faculty → Department → Building → Floor → Room → Laboratory → Storage Area

## 12.1 Location List Page

### Columns

- Location code
- Name
- Type
- Parent location
- Faculty
- Department
- Responsible person
- Asset count
- Active status
- Actions

### Buttons

- Add Location
- View Hierarchy
- Import Locations
- Export

## 12.2 Add Location Page

### Fields

- Location name
- Location code
- Location type
- Parent location
- Campus
- Faculty
- Department
- Building address
- Floor number
- Room number
- Capacity
- Responsible person
- Contact information
- Description
- Active status

### Buttons

- Save
- Save and Add Another
- Cancel

## 12.3 Location Details Page

Tabs:

- Overview
- Assets
- Reservations
- Maintenance
- Transfers
- Activity

---

# 13. Asset Transfer Module

## 13.1 Create Transfer Page

### Fields

- Transfer number
- Asset
- Quantity
- Current faculty
- Current department
- Current location
- Current custodian
- Destination faculty
- Destination department
- Destination location
- New custodian
- Transfer reason
- Requested date
- Expected transfer date
- Notes
- Attachment

### Buttons

- Submit Transfer Request
- Save Draft
- Cancel

## 13.2 Transfer Approval Page

### Buttons

- Approve
- Reject
- Request Changes

## 13.3 Transfer Completion Page

### Fields

- Actual transfer date
- Received by
- Condition at destination
- Notes

### Buttons

- Confirm Transfer
- Cancel

Transfer history must remain permanently available.

---

# 14. Maintenance and Calibration Module

## 14.1 Maintenance Request List

### Columns

- Request number
- Asset
- Issue type
- Priority
- Requested by
- Assigned technician
- Status
- Opened date
- Due date
- Cost
- Actions

## 14.2 Create Maintenance Request

### Fields

- Asset
- Issue type
  - Fault
  - Preventive maintenance
  - Calibration
  - Inspection
  - Cleaning
  - Software update
  - Other
- Issue description
- Priority
- Asset operational status
- Requested by
- Request date
- Preferred service date
- Vendor required
- Photos or documents

### Buttons

- Submit Request
- Save Draft
- Cancel

## 14.3 Maintenance Job Page

### Fields

- Maintenance request
- Assigned technician
- External vendor
- Start date
- Completion date
- Diagnosis
- Work performed
- Parts used
- Labour cost
- Parts cost
- External service cost
- Total cost
- Result
- New condition
- Next service date
- Calibration result
- Certificate upload
- Notes

### Statuses

- Open
- Assigned
- In progress
- Waiting for parts
- Waiting for vendor
- Completed
- Cancelled
- Unrepairable

### Buttons

- Start Work
- Update Job
- Complete Job
- Mark Unrepairable
- Cancel Job

---

# 15. Purchase and Supplier Module

## 15.1 Supplier List Page

### Fields or Columns

- Supplier code
- Supplier name
- Contact person
- Email
- Phone
- Address
- Tax number
- Status
- Rating
- Actions

## 15.2 Add Supplier Page

### Fields

- Supplier name
- Supplier code
- Registration number
- Tax number
- Contact person
- Email
- Phone
- Website
- Address
- City
- Country
- Payment terms
- Notes
- Active status

## 15.3 Purchase Record Page

### Fields

- Purchase order number
- Supplier
- Faculty
- Department
- Purchase date
- Invoice number
- Invoice date
- Currency
- Subtotal
- Tax
- Shipping
- Discount
- Total amount
- Funding source
- Payment status
- Notes
- Invoice upload
- Purchase order upload

### Purchase Item Fields

- Item or asset name
- Category
- Quantity
- Unit price
- Tax
- Total
- Asset creation required

### Buttons

- Save Purchase
- Generate Assets from Purchase
- Cancel

---

# 16. Payments and Charges Module

This module records amounts associated with reservations, setup, equipment use, space use, chemicals, damages, penalties, deposits, and refunds.

## 16.1 Payment List Page

### Columns

- Transaction number
- Transaction type
- User or department
- Related reservation
- Related asset
- Amount
- Currency
- Payment method
- Payment status
- Transaction date
- Recorded by
- Actions

## 16.2 Transaction Types

- Reservation fee
- Equipment usage fee
- Laboratory setup fee
- Room or facility fee
- Consumable charge
- Security deposit
- Damage charge
- Late return penalty
- Refund
- Departmental internal charge
- Other

## 16.3 Add Payment Page

### Fields

- Transaction type
- Payer type
  - User
  - Department
  - Faculty
  - External organization
- Payer
- Related reservation
- Related asset
- Description
- Amount
- Currency
- Payment method
- Reference number
- Payment date
- Status
- Receipt upload
- Notes

### Buttons

- Record Payment
- Save Draft
- Cancel

## 16.4 Refund Page

### Fields

- Original transaction
- Refund amount
- Refund reason
- Refund method
- Refund reference
- Refund date
- Approved by
- Notes

### Buttons

- Process Refund
- Cancel

---

# 17. User and Role Management

## 17.1 User List Page

### Columns

- Name
- User ID
- Email
- Phone
- Role
- Faculty
- Department
- Account status
- Last login
- Actions

### Filters

- Role
- Faculty
- Department
- Status
- Search

### Buttons

- Add User
- Import Users
- Export

## 17.2 Add User Page

### Fields

- First name
- Last name
- University ID
- Email
- Phone
- User type
- Faculty
- Department
- Role
- Supervisor
- Temporary password
- Account active
- Must change password on first login
- Reservation limit
- External borrowing allowed

### Buttons

- Create User
- Create and Add Another
- Cancel

## 17.3 User Details Page

Tabs:

- Profile
- Roles and Permissions
- Reservations
- Checked-Out Assets
- Payments
- Activity Log

Actions:

- Edit User
- Reset Password
- Disable Account
- Unlock Account
- Assign Role

## 17.4 Role Management Page

### Fields

- Role name
- Role description
- Permissions grouped by module

### Permission Types

- View
- Create
- Edit
- Approve
- Archive
- Export
- Manage

---

# 18. Notification Module

## 18.1 Notification Types

- Reservation submitted
- Reservation approved
- Reservation rejected
- Reservation approaching
- Asset ready for collection
- Return due soon
- Return overdue
- Maintenance due
- Calibration due
- Warranty expiring
- Low stock
- Consumable expiring
- Transfer pending approval
- Payment required
- Payment received
- Account-related alert

## 18.2 Notification Channels

- In-application notification
- Email
- Optional SMS

## 18.3 Notification Center Page

### Functions

- View notifications
- Mark as read
- Mark all as read
- Filter by type
- Open linked record
- Delete personal notification

---

# 19. Reports Module

## 19.1 Standard Reports

- Asset register
- Assets by faculty
- Assets by department
- Assets by location
- Assets by category
- Assets by condition
- Asset valuation report
- Depreciation report
- Checked-out assets
- Overdue assets
- Reservation utilization report
- Maintenance history
- Maintenance cost report
- Calibration due report
- Warranty expiry report
- Consumable stock report
- Low-stock report
- Expiry report
- Consumable usage report
- Purchase report
- Supplier report
- Payment and charge report
- Lost and damaged asset report
- Asset transfer report
- User activity report
- Audit report

## 19.2 Report Filters

- Date range
- Faculty
- Department
- Location
- Category
- Asset type
- Status
- Condition
- Supplier
- User

## 19.3 Report Actions

- View
- Export PDF
- Export Excel
- Export CSV
- Print
- Save Report Configuration

---

# 20. Audit Log

The system must maintain an immutable audit trail.

## 20.1 Audit Fields

- Event ID
- Timestamp
- User
- Action
- Module
- Record type
- Record ID
- Previous value
- New value
- IP address
- Device or user agent
- Success or failure

## 20.2 Audited Actions

- Login attempts
- User creation and role changes
- Asset creation and updates
- Asset transfer
- Asset archive
- Reservations and approvals
- Check-outs and returns
- Stock adjustments
- Maintenance changes
- Payments and refunds
- Settings changes

Audit log records must not be editable by normal users.

---

# 21. Settings Module

## 21.1 General Settings

- University name
- University logo
- Default currency
- Default timezone
- Date format
- Language
- Contact email

## 21.2 Asset Settings

- Asset code prefix
- Automatic asset numbering
- Default depreciation method
- QR code format
- Barcode format
- Default maintenance interval

## 21.3 Reservation Settings

- Default maximum duration
- Cancellation window
- Approval requirements
- Reminder timing
- Overdue penalty rules
- External borrowing rules

## 21.4 Inventory Settings

- Low-stock alert rule
- Expiry warning period
- Stock adjustment approval rule

## 21.5 Security Settings

- Password policy
- Session timeout
- Failed login limit
- Account lock duration
- JWT expiry
- Refresh token expiry
- Two-factor authentication option

## 21.6 Notification Settings

- Email templates
- Notification triggers
- Sender email
- SMS provider settings

---

# 22. Database Design

## 22.1 Main Tables

### users

- id UUID primary key
- university_id varchar unique
- first_name varchar
- last_name varchar
- email varchar unique
- phone varchar
- password_hash varchar
- faculty_id UUID nullable
- department_id UUID nullable
- account_status varchar
- last_login_at timestamp nullable
- must_change_password boolean
- created_at timestamp
- updated_at timestamp

### roles

- id UUID
- name varchar unique
- description text
- created_at timestamp

### permissions

- id UUID
- code varchar unique
- module varchar
- action varchar
- description text

### user_roles

- user_id UUID
- role_id UUID

### role_permissions

- role_id UUID
- permission_id UUID

### faculties

- id UUID
- code varchar unique
- name varchar
- description text
- active boolean

### departments

- id UUID
- faculty_id UUID
- code varchar unique
- name varchar
- description text
- active boolean

### locations

- id UUID
- parent_id UUID nullable
- faculty_id UUID nullable
- department_id UUID nullable
- code varchar unique
- name varchar
- type varchar
- address text nullable
- capacity integer nullable
- responsible_user_id UUID nullable
- active boolean

### asset_categories

- id UUID
- parent_id UUID nullable
- code varchar unique
- name varchar
- description text
- asset_type varchar
- active boolean

### assets

- id UUID
- asset_code varchar unique
- name varchar
- description text
- asset_type varchar
- category_id UUID
- brand varchar
- model varchar
- manufacturer varchar
- serial_number varchar nullable
- barcode varchar nullable
- qr_code varchar nullable
- faculty_id UUID
- department_id UUID nullable
- location_id UUID
- custodian_user_id UUID nullable
- supplier_id UUID nullable
- purchase_id UUID nullable
- purchase_date date nullable
- purchase_price decimal nullable
- currency varchar
- current_book_value decimal nullable
- depreciation_method varchar nullable
- useful_life_years integer nullable
- salvage_value decimal nullable
- quantity integer
- available_quantity integer
- condition varchar
- status varchar
- reservable boolean
- approval_required boolean
- external_use_allowed boolean
- deposit_required boolean
- deposit_amount decimal nullable
- max_reservation_hours integer nullable
- warranty_start_date date nullable
- warranty_end_date date nullable
- last_service_date date nullable
- next_service_date date nullable
- calibration_required boolean
- last_calibration_date date nullable
- next_calibration_date date nullable
- archived_at timestamp nullable
- created_by UUID
- created_at timestamp
- updated_at timestamp

### consumable_items

- id UUID
- item_code varchar unique
- name varchar
- description text
- category_id UUID
- faculty_id UUID
- department_id UUID nullable
- location_id UUID
- unit_of_measure varchar
- current_quantity decimal
- reserved_quantity decimal
- reorder_level decimal
- maximum_stock_level decimal nullable
- unit_cost decimal nullable
- hazardous boolean
- chemical_classification varchar nullable
- storage_instructions text nullable
- disposal_instructions text nullable
- active boolean
- created_at timestamp
- updated_at timestamp

### consumable_batches

- id UUID
- consumable_item_id UUID
- batch_number varchar
- quantity_received decimal
- quantity_remaining decimal
- manufacture_date date nullable
- expiry_date date nullable
- unit_cost decimal nullable
- supplier_id UUID nullable
- purchase_id UUID nullable
- received_date date
- created_at timestamp

### stock_transactions

- id UUID
- consumable_item_id UUID
- batch_id UUID nullable
- transaction_type varchar
- quantity decimal
- related_user_id UUID nullable
- related_department_id UUID nullable
- reason text
- reference_number varchar nullable
- created_by UUID
- created_at timestamp

### reservations

- id UUID
- reservation_number varchar unique
- asset_id UUID nullable
- location_id UUID nullable
- requested_by UUID
- faculty_id UUID
- department_id UUID nullable
- purpose text
- course_or_project varchar nullable
- start_at timestamp
- end_at timestamp
- quantity integer
- participant_count integer nullable
- special_requirements text nullable
- external_use_requested boolean
- status varchar
- approval_status varchar
- approved_by UUID nullable
- approved_at timestamp nullable
- approval_notes text nullable
- created_at timestamp
- updated_at timestamp

### checkouts

- id UUID
- checkout_number varchar unique
- reservation_id UUID nullable
- asset_id UUID
- user_id UUID
- quantity integer
- checked_out_at timestamp
- expected_return_at timestamp
- returned_at timestamp nullable
- condition_before varchar
- condition_after varchar nullable
- issued_by UUID
- received_by UUID nullable
- status varchar
- notes text nullable

### maintenance_requests

- id UUID
- request_number varchar unique
- asset_id UUID
- issue_type varchar
- description text
- priority varchar
- requested_by UUID
- assigned_to UUID nullable
- supplier_id UUID nullable
- status varchar
- opened_at timestamp
- due_at timestamp nullable
- started_at timestamp nullable
- completed_at timestamp nullable
- diagnosis text nullable
- work_performed text nullable
- labour_cost decimal nullable
- parts_cost decimal nullable
- external_cost decimal nullable
- total_cost decimal nullable
- result varchar nullable
- next_service_date date nullable

### asset_transfers

- id UUID
- transfer_number varchar unique
- asset_id UUID
- quantity integer
- from_location_id UUID
- to_location_id UUID
- from_custodian_id UUID nullable
- to_custodian_id UUID nullable
- reason text
- status varchar
- requested_by UUID
- approved_by UUID nullable
- received_by UUID nullable
- requested_at timestamp
- approved_at timestamp nullable
- completed_at timestamp nullable

### suppliers

- id UUID
- supplier_code varchar unique
- name varchar
- registration_number varchar nullable
- tax_number varchar nullable
- contact_person varchar nullable
- email varchar nullable
- phone varchar nullable
- website varchar nullable
- address text nullable
- payment_terms varchar nullable
- active boolean

### purchases

- id UUID
- purchase_order_number varchar nullable
- supplier_id UUID
- faculty_id UUID
- department_id UUID nullable
- purchase_date date
- invoice_number varchar nullable
- invoice_date date nullable
- currency varchar
- subtotal decimal
- tax decimal
- shipping decimal
- discount decimal
- total_amount decimal
- funding_source varchar nullable
- payment_status varchar
- created_at timestamp

### payments

- id UUID
- transaction_number varchar unique
- transaction_type varchar
- payer_type varchar
- payer_user_id UUID nullable
- payer_department_id UUID nullable
- reservation_id UUID nullable
- asset_id UUID nullable
- description text
- amount decimal
- currency varchar
- payment_method varchar
- reference_number varchar nullable
- payment_date timestamp
- status varchar
- created_by UUID
- created_at timestamp

### documents

- id UUID
- entity_type varchar
- entity_id UUID
- document_type varchar
- original_filename varchar
- storage_key varchar
- mime_type varchar
- size_bytes bigint
- uploaded_by UUID
- uploaded_at timestamp

### notifications

- id UUID
- user_id UUID
- type varchar
- title varchar
- message text
- entity_type varchar nullable
- entity_id UUID nullable
- read_at timestamp nullable
- created_at timestamp

### audit_logs

- id UUID
- user_id UUID nullable
- action varchar
- module varchar
- entity_type varchar nullable
- entity_id UUID nullable
- old_values jsonb nullable
- new_values jsonb nullable
- ip_address varchar nullable
- user_agent text nullable
- success boolean
- created_at timestamp

---

# 23. REST API Structure

Base URL:

`/api/v1`

## 23.1 Authentication APIs

- POST `/auth/login`
- POST `/auth/refresh`
- POST `/auth/logout`
- POST `/auth/forgot-password`
- POST `/auth/reset-password`
- GET `/auth/me`
- PUT `/auth/change-password`

## 23.2 User APIs

- GET `/users`
- GET `/users/{id}`
- POST `/users`
- PUT `/users/{id}`
- PATCH `/users/{id}/status`
- POST `/users/{id}/roles`
- DELETE `/users/{id}/roles/{roleId}`
- POST `/users/{id}/reset-password`

## 23.3 Asset APIs

- GET `/assets`
- GET `/assets/{id}`
- POST `/assets`
- PUT `/assets/{id}`
- POST `/assets/{id}/archive`
- POST `/assets/{id}/restore`
- GET `/assets/{id}/history`
- GET `/assets/{id}/availability`
- GET `/assets/{id}/documents`
- POST `/assets/{id}/documents`
- GET `/assets/{id}/qr-code`

## 23.4 Consumable APIs

- GET `/consumables`
- GET `/consumables/{id}`
- POST `/consumables`
- PUT `/consumables/{id}`
- POST `/consumables/{id}/receive`
- POST `/consumables/{id}/issue`
- POST `/consumables/{id}/adjust`
- GET `/consumables/{id}/transactions`
- GET `/consumables/low-stock`
- GET `/consumables/expiring`

## 23.5 Reservation APIs

- GET `/reservations`
- GET `/reservations/{id}`
- POST `/reservations`
- PUT `/reservations/{id}`
- POST `/reservations/{id}/submit`
- POST `/reservations/{id}/approve`
- POST `/reservations/{id}/reject`
- POST `/reservations/{id}/cancel`
- GET `/reservations/calendar`
- GET `/reservations/availability`

## 23.6 Check-Out APIs

- GET `/checkouts`
- POST `/checkouts`
- POST `/checkouts/{id}/return`
- POST `/checkouts/{id}/extend`
- GET `/checkouts/overdue`

## 23.7 Maintenance APIs

- GET `/maintenance-requests`
- GET `/maintenance-requests/{id}`
- POST `/maintenance-requests`
- PUT `/maintenance-requests/{id}`
- POST `/maintenance-requests/{id}/assign`
- POST `/maintenance-requests/{id}/start`
- POST `/maintenance-requests/{id}/complete`
- POST `/maintenance-requests/{id}/cancel`

## 23.8 Transfer APIs

- GET `/transfers`
- GET `/transfers/{id}`
- POST `/transfers`
- POST `/transfers/{id}/approve`
- POST `/transfers/{id}/reject`
- POST `/transfers/{id}/complete`

## 23.9 Location APIs

- GET `/locations`
- GET `/locations/tree`
- GET `/locations/{id}`
- POST `/locations`
- PUT `/locations/{id}`
- PATCH `/locations/{id}/status`

## 23.10 Supplier and Purchase APIs

- GET `/suppliers`
- POST `/suppliers`
- GET `/suppliers/{id}`
- PUT `/suppliers/{id}`
- GET `/purchases`
- POST `/purchases`
- GET `/purchases/{id}`
- POST `/purchases/{id}/generate-assets`

## 23.11 Payment APIs

- GET `/payments`
- GET `/payments/{id}`
- POST `/payments`
- POST `/payments/{id}/refund`
- GET `/payments/summary`

## 23.12 Report APIs

- GET `/reports/assets`
- GET `/reports/asset-value`
- GET `/reports/reservations`
- GET `/reports/maintenance`
- GET `/reports/consumables`
- GET `/reports/payments`
- GET `/reports/audit`

Each report endpoint should support output formats such as JSON, CSV, Excel, and PDF where appropriate.

---

# 24. Backend Project Structure

```text
backend/
├── pom.xml
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/com/university/assets/
    │   │   ├── AssetManagementApplication.java
    │   │   ├── config/
    │   │   ├── security/
    │   │   ├── common/
    │   │   │   ├── exception/
    │   │   │   ├── response/
    │   │   │   ├── validation/
    │   │   │   └── util/
    │   │   ├── auth/
    │   │   ├── user/
    │   │   ├── role/
    │   │   ├── faculty/
    │   │   ├── department/
    │   │   ├── location/
    │   │   ├── category/
    │   │   ├── asset/
    │   │   ├── consumable/
    │   │   ├── reservation/
    │   │   ├── checkout/
    │   │   ├── maintenance/
    │   │   ├── transfer/
    │   │   ├── supplier/
    │   │   ├── purchase/
    │   │   ├── payment/
    │   │   ├── document/
    │   │   ├── notification/
    │   │   ├── report/
    │   │   └── audit/
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       └── db/migration/
    └── test/
```

Each feature package should generally contain:

```text
asset/
├── AssetController.java
├── AssetService.java
├── AssetServiceImpl.java
├── AssetRepository.java
├── Asset.java
├── AssetMapper.java
├── dto/
│   ├── AssetCreateRequest.java
│   ├── AssetUpdateRequest.java
│   ├── AssetResponse.java
│   └── AssetSummaryResponse.java
├── specification/
└── validation/
```

---

# 25. Frontend Project Structure

```text
frontend/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── Dockerfile
└── src/
    ├── app/
    │   ├── router.tsx
    │   ├── store.ts
    │   └── providers.tsx
    ├── api/
    ├── assets/
    ├── components/
    │   ├── common/
    │   ├── forms/
    │   ├── tables/
    │   ├── dialogs/
    │   └── layout/
    ├── features/
    │   ├── auth/
    │   ├── dashboard/
    │   ├── assets/
    │   ├── consumables/
    │   ├── reservations/
    │   ├── checkouts/
    │   ├── maintenance/
    │   ├── transfers/
    │   ├── locations/
    │   ├── purchases/
    │   ├── payments/
    │   ├── reports/
    │   ├── users/
    │   ├── audit/
    │   └── settings/
    ├── hooks/
    ├── layouts/
    ├── pages/
    ├── schemas/
    ├── types/
    ├── utils/
    ├── main.tsx
    └── App.tsx
```

---

# 26. UI and User Experience Requirements

- Responsive desktop and tablet layout
- Mobile support for simple tasks such as scanning and returns
- Persistent left sidebar on desktop
- Collapsible sidebar
- Breadcrumbs on internal pages
- Consistent page titles and action buttons
- Confirmation dialog for destructive actions
- Toast messages for success and errors
- Skeleton loaders while fetching data
- Empty-state messages with recommended actions
- Pagination for large tables
- Sortable columns
- Saved filters
- Accessible form labels
- Keyboard navigation
- Clear status badges
- Date and time displayed in the configured timezone

---

# 27. Security Requirements

- Use Spring Security.
- Use BCrypt for password hashing.
- Use short-lived JWT access tokens.
- Use refresh token rotation.
- Store refresh tokens securely.
- Apply role and permission checks to every protected endpoint.
- Validate all incoming data.
- Prevent SQL injection through parameterized JPA queries.
- Apply file type and file size restrictions.
- Protect against cross-site scripting.
- Configure CORS strictly.
- Rate-limit login and password-reset endpoints.
- Record failed login attempts.
- Lock users after repeated failed attempts.
- Avoid exposing stack traces in production.
- Store secrets in environment variables.
- Use HTTPS in production.

---

# 28. Validation and Business Rules

## Assets

- Asset code must be unique.
- Quantity cannot fall below checked-out or reserved quantity.
- Lost, archived, or disposed assets cannot be reserved.
- An asset under maintenance is unavailable.
- Location transfers require authorization.

## Consumables

- Stock cannot become negative.
- Expired chemical batches cannot be issued.
- FIFO or FEFO should be used for batch issuing.
- Hazardous items may require additional approval.

## Reservations

- Start date must be before end date.
- Reservations cannot conflict beyond available capacity.
- Users must not exceed their reservation limits.
- External use requires permission.
- A required deposit must be paid before check-out.

## Returns

- Damage must be recorded with a condition and description.
- Late penalties should be calculated according to configuration.
- Missing accessories should be recorded.

## Payments

- Amount must be greater than zero except for adjustments.
- Refunds cannot exceed the paid amount.
- Each payment requires an auditable reference.

---

# 29. File Upload Requirements

Allowed document formats:

- PDF
- JPG
- JPEG
- PNG
- DOCX
- XLSX

Recommended restrictions:

- Maximum file size: 10 MB per file
- Virus scan in production
- Store generated file name separately from original file name
- Do not expose direct storage paths
- Require permission before downloading protected documents

---

# 30. Search Requirements

Global search should support:

- Asset code
- Asset name
- Serial number
- Barcode
- QR code
- Reservation number
- Checkout number
- User name
- University ID
- Location
- Supplier
- Purchase order number
- Invoice number

Search results should be grouped by record type.

---

# 31. Import and Export Requirements

## Imports

- Asset import from CSV or Excel
- Consumable import
- User import
- Location import

Import process:

1. Download template
2. Upload completed file
3. Validate file
4. Display errors by row
5. Preview valid records
6. Confirm import
7. Generate import result report

## Exports

- CSV
- Excel
- PDF for reports

Exports must respect current filters and user permissions.

---

# 32. Scheduled Jobs

Use Spring Scheduler or Quartz for:

- Reservation reminders
- Overdue reminders
- Maintenance due alerts
- Calibration due alerts
- Warranty expiry alerts
- Low-stock alerts
- Expiry alerts
- Daily report summaries
- Cleanup of expired authentication tokens

---

# 33. Testing Requirements

## Backend Tests

- Unit tests for services
- Repository integration tests
- Controller tests
- Security authorization tests
- Reservation conflict tests
- Stock transaction tests
- Payment and refund tests
- Testcontainers for PostgreSQL integration tests

## Frontend Tests

- Component tests
- Form validation tests
- Permission-based rendering tests
- API error handling tests
- End-to-end tests using Playwright or Cypress

## Required Critical Test Cases

- Duplicate asset code rejected
- Reservation conflict rejected
- Asset under maintenance unavailable
- Negative stock prevented
- Expired chemical cannot be issued
- Unauthorized user cannot approve reservation
- Refund greater than original payment rejected
- Audit log created after asset update
- Overdue return calculated correctly

---

# 34. API Response Format

Successful response:

```json
{
  "success": true,
  "message": "Asset created successfully",
  "data": {},
  "timestamp": "2026-07-28T00:00:00+10:00"
}
```

Error response:

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "assetCode": "Asset code already exists"
  },
  "timestamp": "2026-07-28T00:00:00+10:00"
}
```

Paginated response:

```json
{
  "success": true,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

---

# 35. Initial Seed Data

The development database should include:

- One university
- One campus
- Science Faculty
- Chemistry Department
- Physics Department
- Biology Department
- Computer Science Department
- Example laboratories
- Example stores and rooms
- Default roles
- Default permissions
- One super administrator
- Example assets
- Example consumables
- Example reservations

Development administrator:

- Email: `admin@university.local`
- Password: loaded from an environment variable

Never hard-code production credentials.

---

# 36. Docker Compose Services

The project should include:

- PostgreSQL database
- Spring Boot backend
- React frontend
- Nginx reverse proxy
- Optional MailHog for local email testing
- Optional MinIO for document storage testing

Example service names:

```text
postgres
backend
frontend
nginx
mailhog
minio
```

---

# 37. Environment Variables

## Backend

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_ACCESS_EXPIRY`
- `JWT_REFRESH_EXPIRY`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `STORAGE_TYPE`
- `STORAGE_PATH`
- `S3_ENDPOINT`
- `S3_BUCKET`
- `S3_ACCESS_KEY`
- `S3_SECRET_KEY`
- `APP_FRONTEND_URL`

## Frontend

- `VITE_API_BASE_URL`

---

# 38. Development Milestones

## Phase 1: Foundation

- Create Git repository
- Create backend and frontend projects
- Configure PostgreSQL
- Configure Docker Compose
- Configure Flyway
- Implement global error handling
- Implement API response model

## Phase 2: Authentication and Authorization

- Login
- JWT
- Refresh token
- Password reset
- User management
- Roles and permissions

## Phase 3: Organization Structure

- Faculties
- Departments
- Locations
- Location hierarchy

## Phase 4: Core Asset Management

- Asset categories
- Add asset
- Edit asset
- Asset list
- Asset details
- Documents
- QR labels
- Archive

## Phase 5: Consumable Inventory

- Consumable items
- Batches
- Receiving stock
- Issuing stock
- Adjustments
- Low-stock and expiry alerts

## Phase 6: Reservations

- Availability checking
- Reservation creation
- Approval workflow
- Calendar
- Notifications

## Phase 7: Check-Out and Returns

- Check-out
- Return
- Condition records
- Overdue tracking
- Penalty calculation

## Phase 8: Maintenance and Transfers

- Maintenance requests
- Maintenance jobs
- Calibration
- Asset transfers

## Phase 9: Purchases and Payments

- Suppliers
- Purchases
- Charges
- Deposits
- Refunds

## Phase 10: Reports, Audit, and Dashboard

- Dashboard cards
- Charts
- Reports
- Exports
- Audit log

## Phase 11: Testing and Deployment

- Unit tests
- Integration tests
- End-to-end tests
- Security testing
- Docker production build
- Deployment documentation

---

# 39. Minimum Viable Product

The first usable release must include:

- Login and password reset
- User roles and permissions
- Faculty, department, and location management
- Fixed asset CRUD
- Consumable CRUD
- Asset search and filtering
- Reservation creation and approval
- Check-out and return
- Consumable receiving and issuing
- Maintenance request
- Basic dashboard
- Basic reports
- Audit logging

---

# 40. Future Enhancements

- Native mobile application
- NFC asset scanning
- RFID integration
- University single sign-on
- LDAP or Active Directory integration
- Two-factor authentication
- IoT equipment monitoring
- Automatic depreciation calculations
- Procurement approval workflow
- Budget management
- Predictive maintenance
- AI-assisted asset categorization
- Offline scanning mode
- Electronic signatures
- Integration with student information systems
- Integration with finance and ERP systems

---

# 41. Coding Agent Instructions

The coding agent should:

1. Use a modular monolith architecture for the first release.
2. Use Java 21 and Spring Boot 3.x.
3. Use PostgreSQL unless MySQL is explicitly selected.
4. Use React and TypeScript for the frontend.
5. Use UUID primary keys.
6. Use Flyway migrations for every schema change.
7. Use DTOs instead of exposing JPA entities.
8. Implement pagination, sorting, and filtering for list endpoints.
9. Add OpenAPI documentation.
10. Add unit and integration tests for critical workflows.
11. Add Dockerfiles and Docker Compose.
12. Add a root README with setup instructions.
13. Add sample `.env.example` files.
14. Never hard-code credentials or secrets.
15. Implement audit logging through a reusable service or aspect.
16. Implement permission checks both in the UI and backend.
17. Use soft deletion or archival for important business records.
18. Store dates and times consistently and return ISO 8601 values.
19. Keep financial values in decimal database types, never floating point.
20. Create clean commits by feature or milestone.

---

# 42. Definition of Done

A feature is complete when:

- Backend endpoint is implemented.
- Permission checks are implemented.
- Validation is implemented.
- Database migration is included.
- Frontend page and form are implemented.
- Loading, empty, success, and error states are handled.
- Audit logging is added where required.
- Unit or integration tests are added.
- API documentation is updated.
- The feature works in Docker Compose.

---

# 43. Expected Final Repository

```text
university-asset-management/
├── README.md
├── docker-compose.yml
├── .env.example
├── docs/
│   ├── system-specification.md
│   ├── api-design.md
│   ├── database-design.md
│   └── deployment-guide.md
├── backend/
├── frontend/
├── nginx/
└── scripts/
```

The root README should explain:

- Project purpose
- Technology stack
- Local setup
- Environment configuration
- Database migration
- Running tests
- Starting Docker Compose
- Default development account
- API documentation URL

---

# 44. Final Project Summary

Build a secure, modular university asset management platform that allows the science faculty to manage fixed assets, consumables, laboratories, equipment reservations, usage, payments, maintenance, locations, users, and reports.

The system must make it easy to answer these questions:

- What assets does the university own?
- Where is each asset located?
- Who is responsible for it?
- What is its quantity, condition, value, and availability?
- Who has reserved or borrowed it?
- When must it be returned?
- What maintenance has been performed?
- What chemicals and consumables are available?
- Which items are low in stock or expiring?
- What payments, deposits, or charges are connected to an asset or reservation?
- What changes have been made, and by whom?

This specification should be treated as the main source of truth for the first implementation.
