# 🚀 SmartATS — AI-Powered Recruitment & Interview Intelligence Platform

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-323330?style=for-the-badge&logo=javascript&logoColor=F7DF1E)
![Vite](https://img.shields.io/badge/Vite-B73BFE?style=for-the-badge&logo=vite&logoColor=FFD62E)
![Render](https://img.shields.io/badge/Render-46E3B7?style=for-the-badge&logo=render&logoColor=white)

## 📖 Project Overview

Welcome to **SmartATS**, a next-generation AI-powered recruitment and applicant tracking system designed to streamline the hiring process from start to finish. 

In today's fast-paced job market, recruiters are overwhelmed with applications, and candidates struggle to get their resumes noticed. SmartATS solves this by bridging the gap between candidates and companies through intelligent AI automation. We empower candidates to optimize their resumes for ATS (Applicant Tracking Systems) and practice mock interviews, while simultaneously equipping recruiters with the tools to intelligently filter, analyze, and manage top talent.

**Key capabilities include:**
- **AI ATS Analysis:** Intelligent parsing and scoring of candidate resumes.
- **AI Mock Interviews:** Practice domain-specific interviews with live AI evaluation.
- **Resume Matching:** Automated scoring of candidate resumes against job descriptions.
- **Interview Scheduling:** Streamlined recruitment workflows and notifications.
- **Security Monitoring:** Anti-cheating and proctoring during mock interviews.
- **DSA/AST Analysis Engine (Upcoming):** Next-gen visual dry-runs for data structures and algorithms.

---

## ✨ Key Features

### 👨‍💼 Candidate Features
| Feature | Description |
|---------|-------------|
| 📄 **Resume Upload** | Easily upload resumes (PDF/DOCX) for AI processing. |
| 📊 **ATS Analysis** | Get instant feedback on resume keywords and formatting. |
| 💼 **Job Applications** | Apply to roles seamlessly with ATS-optimized profiles. |
| 🤖 **AI Mock Interview** | Practice behavioral and technical rounds with an AI interviewer. |
| 📷 **Webcam Monitoring** | Secure interview environment with visual proctoring. |
| 🚫 **Tab Switch Detection** | Anti-cheating measures during active assessments. |
| 📅 **Interview Schedules** | Automated notifications and scheduling coordination. |
| 📑 **PDF Report Generation** | Downloadable ATS snapshot reports. |
| 🔐 **OTP Verification** | Secure email-based registration and account recovery. |
| 🛡️ **CAPTCHA Protection** | Bot prevention for secure access. |

### 🏢 Recruiter Features
| Feature | Description |
|---------|-------------|
| 📝 **Create Jobs** | Post new job openings with required skills and descriptions. |
| 👥 **View Applicants** | Centralized dashboard for managing candidate pipelines. |
| 🎯 **ATS Score Visibility** | Quickly see how well a candidate matches the job. |
| ✅ **Shortlist/Reject** | Streamlined candidate tracking and status management. |
| 🗓️ **Schedule Interviews** | Invite candidates to technical or behavioral rounds. |
| 📥 **Resume Download** | Access original candidate documents in one click. |
| 📈 **Applicant Analytics** | Data-driven insights into hiring pipelines. |

### 🧠 AI Features
- **ATS Keyword Analysis:** Uses Natural Language Processing to extract and match skills.
- **Resume Scoring:** Generates a match percentage based on the job description.
- **AI-Generated Questions:** Dynamic interview questions tailored to the candidate's resume and the job role.
- **AI Evaluation Support:** Automated feedback on interview responses.

---

## 🚀 Upcoming Advanced Features (Roadmap)

We are constantly evolving. Our upcoming **AlgoMind DSA Engine** will revolutionize technical screening:
- **Visual DSA Dry-Run Engine:** Step-by-step visualizations of algorithms.
- **AST-Based Code Analysis:** Deep parsing of candidate code submissions.
- **Recursion Tree Generation:** Visual debugging for complex recursive functions.
- **Plain-English Big-O Explanation:** AI-generated complexity analysis.
- **Edge-Case Generator:** Automated test cases for edge-case validation.
- **AI Debugging Assistant:** Real-time hints without giving away the answer.
- **Real-Time Collaborative Rooms:** Live coding environments for human interviewers.
- **AI Coding Interviewer:** Fully automated technical screening rounds.

---

## 🏗️ System Architecture

SmartATS is built on a robust, scalable, and modern tech stack.

### 💻 Frontend
- **Tech:** HTML5, Vanilla CSS, JavaScript, Vite
- **Architecture:** Modular, component-driven Vanilla JS with dynamic DOM updates.

### ⚙️ Backend
- **Framework:** Spring Boot (Java 17)
- **Architecture:** RESTful APIs, Service-Oriented Architecture (SOA).
- **Security:** Spring Security, JWT Authentication, BCrypt Password Hashing.
- **ORM:** JPA / Hibernate for robust data management.

### 🗄️ Database
- **Provider:** PostgreSQL (hosted on Supabase)
- **Migrations:** Flyway for automated schema versioning.

### ☁️ Deployment
- **Backend:** Render (Dockerized/Native)
- **Frontend:** Netlify / Vercel
- **Storage:** Local / Cloud-based Document Storage

---

## 🔒 Security Features

Security is a first-class citizen in SmartATS:
- **BCrypt Password Hashing:** Ensuring user credentials are safely stored.
- **JWT Authentication:** Stateless, secure API access with short-lived tokens.
- **CAPTCHA:** Mitigating bot attacks on authentication endpoints.
- **OTP Email Verification:** Secure registration and password reset workflows.
- **Protected APIs:** Comprehensive endpoint security preventing IDOR and unauthorized access.
- **Role-Based Access Control (RBAC):** Strict separation between `CANDIDATE`, `RECRUITER`, and `ADMIN` roles.

---

## ⚙️ ATS Engine Explanation

The core of our platform is the intelligent ATS Engine:
1. **Keyword Extraction:** Parses resumes to identify core competencies and tools.
2. **Formatting Analysis:** Ensures resumes meet industry-standard readability guidelines.
3. **Scoring Model:** Calculates a proprietary match score based on semantic similarity to the job description.
4. **Match Snapshots:** Captures immutable records of a candidate's ATS score at the time of application.
5. **Recruiter Visibility:** Surfaces top candidates dynamically on the recruiter dashboard.
6. **PDF Report Generation:** Compiles findings into a clean, downloadable PDF report.

---

## 🎙️ Mock Interview Engine

Our AI Mock Interview system simulates real-world pressure:
- **Webcam Monitoring:** Requests camera access to ensure a proctored environment.
- **Microphone Support:** Captures audio for speech-to-text processing (future integration).
- **Tab Switching Detection:** Logs and flags if a candidate leaves the active assessment tab.
- **Live Transcript:** Records the Q&A session for recruiter review.
- **Anti-Cheating Measures:** Comprehensive behavioral tracking during the session.
- **Interview Analytics:** Summarizes performance, confidence, and technical accuracy.

---

## 📸 Screenshots

*(Placeholders for future UI snapshots)*

| Home Page | ATS Checker | Recruiter Dashboard |
|-----------|-------------|---------------------|
| *[Image Placeholder]* | *[Image Placeholder]* | *[Image Placeholder]* |

| Candidate Dashboard | Mock Interview | Job Applications |
|---------------------|----------------|------------------|
| *[Image Placeholder]* | *[Image Placeholder]* | *[Image Placeholder]* |

---

## 🛠️ Installation Guide

### Prerequisites
- Java 17+
- Node.js & npm
- PostgreSQL

### 🖥️ Backend Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/SAICHARAN1205/AI-Interview-System-Smart-ATS.git
   cd AI-Interview-System-Smart-ATS
   ```
2. Configure your `application.properties` (see Environment Variables).
3. Run the Spring Boot application:
   ```bash
   ./mvnw spring-boot:run
   ```

### 🎨 Frontend Setup
1. Navigate to the frontend directory (if separated) or serve the static files:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

---

## 🔑 Environment Variables

To run the backend, create a `.env` file or export the following variables:

```env
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
SPRING_DATASOURCE_URL=jdbc:postgresql://your-supabase-url:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.your_db_user
SPRING_DATASOURCE_PASSWORD=your_db_password
JWT_SECRET=your_super_secret_jwt_key_256_bits
```

---

## 🗃️ Database Schema Overview

The platform relies on a normalized relational database:
- `users`: Core identity table (Candidates, Recruiters, Admins).
- `jobs`: Job postings created by recruiters.
- `applications`: Tracks the many-to-many relationship between users and jobs.
- `ats_analysis_snapshots`: Immutable records of resume scores for specific applications.
- `interview_schedules`: Manages upcoming interview appointments and statuses.
- `resumes`: Metadata and storage references for candidate documents.

---

## 🧩 Engineering Challenges Solved

Building SmartATS required overcoming significant technical hurdles:
- **Robust OTP Flow Handling:** Implementing atomic transactions to prevent controller crashes on SMTP failures and enforcing strict request cooldowns.
- **ATS Score Persistence:** Designing immutable snapshots so recruiter views remain historically accurate even if candidates update their resumes later.
- **Recruiter/Candidate Sync:** Ensuring state consistency across disparate user roles in a stateless JWT environment.
- **Interview Scheduling Validation:** Preventing time conflicts and handling timezone offsets seamlessly.
- **Anti-Cheating Monitoring:** Hooking into browser Page Visibility APIs to accurately flag tab-switching behavior without false positives.
- **PDF Generation:** Assembling complex HTML-to-PDF reports on the server side efficiently.
- **Supabase Integration:** Managing connection pooling (HikariCP) against Supabase's serverless Postgres infrastructure.

---

## 🌍 Deployment Links

- **Frontend Deployment:** *[Insert Frontend Link]*
- **Backend API:** *[Insert Backend Link]*
- **API Documentation (Swagger/Postman):** *[Insert API Docs Link]*

---

## 🔭 Future Scalability Scope

As the platform grows, we are preparing for:
- **Redis Integration:** For lightning-fast session caching and rate-limiting.
- **WebSockets:** Enabling real-time chat, notifications, and live collaborative coding.
- **Docker & Kubernetes:** Container orchestration for microservices scaling.
- **AI Code Analysis Engine:** Deeper AST integration for automated technical screening.
- **Live Collaborative Editor:** Operational transformation (OT) for simultaneous multi-user text editing.

---

## 👨‍💻 Contributors

- **Sai Charan** - *Lead Developer* - [GitHub](https://github.com/SAICHARAN1205)

---

## 📄 License

This project is licensed under the **MIT License**. See the `LICENSE` file for more details.
