import json
import os

os.makedirs('app/src/main/assets/data', exist_ok=True)

packages = [
    {
        'id': 'freshman',
        'name': 'Freshman Courses',
        'shortName': 'Freshman',
        'description': 'All 20+ common first-year university courses for Natural & Social streams with official textbooks, lecture notes, question banks, and worked exams.',
        'path': '/academy/freshman',
        'image': 'code-cover://freshman',
        'enrolledLabel': '520+ scholars',
        'group': 'branch',
        'sortOrder': 1
    },
    {
        'id': 'ece-y3',
        'name': 'Electrical & Computer Engineering',
        'shortName': 'ECE',
        'description': 'Senior Electrical and Computer Engineering track: Semester 1 & Semester 2 courses, notes, question banks, and exams.',
        'path': '/academy/special-packages/electrical-computer-engineering',
        'image': 'code-cover://ece',
        'enrolledLabel': '240+ scholars',
        'group': 'special',
        'sortOrder': 2
    },
    {
        'id': 'grade-12',
        'name': 'Grade 12 Package',
        'shortName': 'G12',
        'description': 'Peak year: leaving-exam drills, university entrance prep, chapter summaries, and timed model exams with full solutions.',
        'path': '/academy/grades/12',
        'image': 'code-cover://grade-12',
        'enrolledLabel': '610+ scholars',
        'group': 'grades',
        'sortOrder': 3
    },
    {
        'id': 'grade-11',
        'name': 'Grade 11 Package',
        'shortName': 'G11',
        'description': 'Natural & Social science stream depth, high-yield summaries, formula recall flashcards, and exam-level problems.',
        'path': '/academy/grades/11',
        'image': 'code-cover://grade-11',
        'enrolledLabel': '480+ scholars',
        'group': 'grades',
        'sortOrder': 4
    },
    {
        'id': 'grade-10',
        'name': 'Grade 10 Package',
        'shortName': 'G10',
        'description': 'Concept mastery, stream selection preparation drills, chapter question banks, and solved practice exams.',
        'path': '/academy/grades/10',
        'image': 'code-cover://grade-10',
        'enrolledLabel': '410+ scholars',
        'group': 'grades',
        'sortOrder': 5
    },
    {
        'id': 'grade-9',
        'name': 'Grade 9 Package',
        'shortName': 'G9',
        'description': 'Start secondary with material built for Grade 9. Clear explanations, chapter practice, and exam-style questions.',
        'path': '/academy/grades/9',
        'image': 'code-cover://grade-9',
        'enrolledLabel': '320+ scholars',
        'group': 'grades',
        'sortOrder': 6
    },
    {
        'id': 'uat',
        'name': 'AAU UAT Entrance Exam',
        'shortName': 'UAT',
        'description': 'Undergraduate Admission Test preparation covering quantitative reasoning, verbal problem solving, and analytical drills.',
        'path': '/academy/uat',
        'image': 'code-cover://uat',
        'enrolledLabel': '390+ scholars',
        'group': 'branch',
        'sortOrder': 7
    },
    {
        'id': 'coc',
        'name': 'COC Assessment',
        'shortName': 'COC',
        'description': 'Occupational standard competence assessments with practical revision guides and question banks.',
        'path': '/academy/coc',
        'image': 'code-cover://coc',
        'enrolledLabel': '280+ scholars',
        'group': 'branch',
        'sortOrder': 8
    },
    {
        'id': 'gat',
        'name': 'AAU GAT Graduate Aptitude',
        'shortName': 'GAT',
        'description': 'Graduate Admission Test practice sets, analytical reasoning drills, and timed simulations.',
        'path': '/academy/gat',
        'image': 'code-cover://gat',
        'enrolledLabel': '220+ scholars',
        'group': 'branch',
        'sortOrder': 9
    },
    {
        'id': 'exit-exam',
        'name': 'Exit Exam',
        'shortName': 'Exit',
        'description': 'National university exit examination materials to consolidate your field of study.',
        'path': '/academy/exit-exam',
        'image': 'code-cover://exit-exam',
        'enrolledLabel': '340+ scholars',
        'group': 'branch',
        'sortOrder': 10
    },
    {
        'id': 'remedial',
        'name': 'Remedial Program',
        'shortName': 'Remedial',
        'description': 'Core prerequisite subject strengthening for university transition and placement success.',
        'path': '/academy/remedial',
        'image': 'code-cover://remedial',
        'enrolledLabel': '260+ scholars',
        'group': 'branch',
        'sortOrder': 11
    }
]

freshman_courses = [
    ('math-natural', 'Math Natural', 'Calculus, analytic geometry, vectors, matrices, and algebraic systems for STEM disciplines.'),
    ('math-social', 'Math Social', 'Algebraic foundations, linear programming, financial mathematics, and statistics for social sciences.'),
    ('physical-fitness', 'Physical Fitness', 'Health-related fitness, cardiovascular endurance, strength development, and personal wellness.'),
    ('english-1', 'English 1', 'Academic reading strategies, grammatical structures, paragraph mechanics, and expository writing.'),
    ('physics', 'Physics', 'Classical mechanics, vectors, motion, work-energy theorem, fluid dynamics, and thermodynamics.'),
    ('psychology', 'Psychology', 'Biological bases of behavior, cognitive psychology, learning theories, motivation, and mental health.'),
    ('logic', 'Logic', 'Formal propositions, truth tables, categorical logic, syllogistic deductions, and fallacies.'),
    ('geography', 'Geography', 'Physical landscape processes, climate dynamics, population distribution, and resource management.'),
    ('anthropology', 'Anthropology', 'Human cultural evolution, ethnographic methods, indigenous traditions, and societal institutions.'),
    ('civics', 'Civics', 'Constitutional principles, federal governance structure, human rights jurisprudence, and rule of law.'),
    ('economics', 'Economics', 'Microeconomic behavior, consumer theory, and macroeconomic aggregates (GDP, fiscal policy).'),
    ('emerging-technology', 'Emerging Technology', 'Architectures of AI, data science, IoT networks, cloud computing, and cybersecurity.'),
    ('entrepreneurship', 'Entrepreneurship', 'Venture ideation, business model design, Ethiopian regulatory frameworks, and financial planning.'),
    ('global-trends', 'Global Trends', 'Post-Cold War international order, regional cooperation in East Africa, and environmental diplomacy.'),
    ('history', 'History', 'Historiography, human origins in the Horn, state formation, and Ethiopian modernization.'),
    ('inclusiveness', 'Inclusiveness', 'Inclusive education principles, supporting students with disabilities, and universal design.'),
    ('cpp-programming', 'C++ Programming', 'Algorithm design, syntax structures, object-oriented concepts, pointers, and memory management.')
]

courses = []
for idx, (cid, cname, cdesc) in enumerate(freshman_courses):
    courses.append({
        'id': f'freshman-{cid}',
        'packageId': 'freshman',
        'semesterId': None,
        'name': cname,
        'code': None,
        'description': cdesc,
        'image': f'code-cover://freshman/{cid}',
        'path': f'/academy/freshman/{cid}',
        'sortOrder': idx + 1
    })

# ECE S1
ece_s1 = [
    ('ECEg3071', 'Applied Electronics II', 'eceg3071'),
    ('Econ1011', 'Economics', 'econ1011'),
    ('ECEg3051', 'Electromagnetic Fields', 'eceg3051'),
    ('ECEg3081', 'Signals and Systems Analysis', 'eceg3081'),
    ('ECEg3073', 'Electrical Engineering Laboratory III', 'eceg3073'),
    ('ECEg3101', 'Object Oriented Programming', 'eceg3101'),
    ('ECEg3061', 'Computational Methods', 'eceg3061')
]
for idx, (code, title, slug) in enumerate(ece_s1):
    courses.append({
        'id': f'ece-s1-{slug}',
        'packageId': 'ece-y3',
        'semesterId': 'sem-1',
        'name': title,
        'code': code,
        'description': f'{code} senior semester module for Electrical & Computer Engineering.',
        'image': f'code-cover://course/{slug}',
        'path': f'/academy/special-packages/electrical-computer-engineering/sem-1/{slug}',
        'sortOrder': idx + 1
    })

# ECE S2
ece_s2 = [
    ('MEng3052', 'Engineering Thermodynamics', 'meng3052'),
    ('ECEg3082', 'Network Analysis and Synthesis', 'eceg3082'),
    ('ECEg3092', 'Introduction to Electrical Machines', 'eceg3092'),
    ('ECEg3094', 'Electrical Engineering Lab IV', 'eceg3094'),
    ('ECEg3102', 'Digital Logic Design', 'eceg3102'),
    ('ECEg3052', 'Electrical Materials and Technology', 'eceg3052'),
    ('ECEg3096', 'Electrical Workshop Practice II', 'eceg3096')
]
for idx, (code, title, slug) in enumerate(ece_s2):
    courses.append({
        'id': f'ece-s2-{slug}',
        'packageId': 'ece-y3',
        'semesterId': 'sem-2',
        'name': title,
        'code': code,
        'description': f'{code} senior semester module for Electrical & Computer Engineering.',
        'image': f'code-cover://course/{slug}',
        'path': f'/academy/special-packages/electrical-computer-engineering/sem-2/{slug}',
        'sortOrder': idx + 1
    })

# Grade 12 Subjects
g12_subs = [
    ('mathematics', 'Mathematics', 'Exam-ready pure and applied math'),
    ('physics', 'Physics', 'Electromagnetism, modern physics, nuclear physics'),
    ('chemistry', 'Chemistry', 'Organic, analytical, and physical chemistry'),
    ('biology', 'Biology', 'Molecular genetics, human physiology, ecology'),
    ('english', 'English', 'Matriculation reading comprehension, syntax and rhetoric'),
    ('economics', 'Economics', 'Macroeconomic models and development finance'),
    ('geography', 'Geography', 'Geomorphology, climate change, and demographic analysis'),
    ('history', 'History', 'Modern Ethiopian diplomacy and world conflicts')
]
for idx, (sid, sname, sdesc) in enumerate(g12_subs):
    courses.append({
        'id': f'grade-12-{sid}',
        'packageId': 'grade-12',
        'semesterId': None,
        'name': sname,
        'code': None,
        'description': sdesc,
        'image': f'code-cover://grade-12/g12-{sid}',
        'path': f'/academy/grades/12/{sid}',
        'sortOrder': idx + 1
    })

# Remedial Subjects
rem_subs = [
    ('english', 'English', 'Grammar fundamentals, reading comprehension, vocabulary expansion, and essay writing.'),
    ('maths', 'Maths', 'Algebraic simplification, polynomials, quadratic equations, and logarithms.'),
    ('physics', 'Physics', 'Kinematics, Newton laws, gravitation, and introductory electrostatics.'),
    ('chemistry', 'Chemistry', 'Periodic trends, chemical equations, stoichiometric calculations.'),
    ('biology', 'Biology', 'Cell structure and function, basic taxonomy, and human organ systems.'),
    ('history', 'History', 'Modern Ethiopian history, major transitions, and regional interactions.'),
    ('geography', 'Geography', 'Map reading skills, Ethiopian physical geography, and demographics.')
]
for idx, (sid, sname, sdesc) in enumerate(rem_subs):
    courses.append({
        'id': f'remedial-{sid}',
        'packageId': 'remedial',
        'semesterId': None,
        'name': sname,
        'code': None,
        'description': sdesc,
        'image': f'code-cover://remedial/{sid}',
        'path': f'/academy/remedial/{sid}',
        'sortOrder': idx + 1
    })

# Learning Resources
learning_resources = [
    # Freshman Math Natural
    {
        'id': 'fn-math-book-1',
        'packageId': 'freshman',
        'scopePath': 'freshman/math-natural',
        'hub': 'books',
        'title': 'Mathematics for Natural Sciences (Freshman Official)',
        'chapter': 1,
        'sortOrder': 1,
        'contentType': 'pdf',
        'storagePath': 'freshman/math-natural/official_math_natural_textbook.pdf',
        'bodyMd': None,
        'metaJson': json.dumps({'author': 'Ministry of Education', 'pages': 284}),
        'published': True
    },
    {
        'id': 'fn-math-notes-1',
        'packageId': 'freshman',
        'scopePath': 'freshman/math-natural',
        'hub': 'short-notes',
        'title': 'Chapter 1: Propositional Logic and Set Theory Summaries',
        'chapter': 1,
        'sortOrder': 1,
        'contentType': 'markdown',
        'storagePath': None,
        'bodyMd': '# Chapter 1: Propositional Logic & Set Theory\n\n## 1.1 Propositions and Truth Tables\nA **proposition** is a declarative statement that is either true or false, but not both.\n\n### Logical Connectives:\n- **Conjunction (P ∧ Q):** True only when both P and Q are true.\n- **Disjunction (P ∨ Q):** False only when both P and Q are false.\n- **Conditional (P ⇒ Q):** False only when P is true and Q is false.\n- **Biconditional (P ⇔ Q):** True when both share the identical truth value.\n\n## 1.2 Quantifiers\n- **Universal (∀x):** States that the property holds for all elements in the domain.\n- **Existential (∃x):** States that there exists at least one element satisfying the property.',
        'metaJson': json.dumps({'author': 'Wisdom Tower Faculty', 'readMinutes': 8}),
        'published': True
    },
    {
        'id': 'fn-math-flashcards-1',
        'packageId': 'freshman',
        'scopePath': 'freshman/math-natural',
        'hub': 'flashcards',
        'title': 'Logic & Set Theory Fast Recall Deck',
        'chapter': 1,
        'sortOrder': 1,
        'contentType': 'flashcard_deck',
        'storagePath': None,
        'bodyMd': None,
        'metaJson': json.dumps({
            'cards': [
                {'front': 'What is the negation of (P AND Q)?', 'back': 'NOT P OR NOT Q (De Morgan\'s Law)'},
                {'front': 'When is P implies Q false?', 'back': 'Only when P is True and Q is False.'},
                {'front': 'What is a tautology?', 'back': 'A compound proposition that is always true regardless of the truth values of its variables.'},
                {'front': 'What is the contrapositive of P -> Q?', 'back': 'NOT Q -> NOT P (logically equivalent to P -> Q)'}
            ]
        }),
        'published': True
    },
    {
        'id': 'fn-math-banks-1',
        'packageId': 'freshman',
        'scopePath': 'freshman/math-natural',
        'hub': 'question-banks',
        'title': 'Propositional Logic & Quantifiers Chapter Bank (Worked)',
        'chapter': 1,
        'sortOrder': 1,
        'contentType': 'markdown',
        'storagePath': None,
        'bodyMd': '# Question Bank: Logic & Quantifiers\n\n**Q1:** Let P and Q be propositions. If P is false and Q is true, find the truth value of (¬P ∨ Q) ⇒ (P ∧ Q).\n\n*Solution:*\n1. ¬P = ¬F = T\n2. ¬P ∨ Q = T ∨ T = T\n3. P ∧ Q = F ∧ T = F\n4. Therefore, T ⇒ F = F.\n**Answer: False**',
        'metaJson': json.dumps({'questionsCount': 25}),
        'published': True
    },
    {
        'id': 'fn-math-exams-1',
        'packageId': 'freshman',
        'scopePath': 'freshman/math-natural',
        'hub': 'exams',
        'title': 'Addis Ababa University Official Midterm Exam (Worked)',
        'chapter': None,
        'sortOrder': 1,
        'contentType': 'markdown',
        'storagePath': None,
        'bodyMd': '# AAU Freshman Math Natural — Midterm Exam\n\n### Part I: Logic and Relations (30 Marks)\nComprehensive worked solutions with full step-by-step proofs for university midterm assessments across AAU, ASTU, and AASTU.',
        'metaJson': json.dumps({'year': '2024/2025', 'institution': 'AAU'}),
        'published': True
    },
    {
        'id': 'fn-math-lifesavers-1',
        'packageId': 'freshman',
        'scopePath': 'freshman/math-natural',
        'hub': 'life-savers',
        'title': 'Calculus & Vectors One-Page Exam Formula Cheat Sheet',
        'chapter': None,
        'sortOrder': 1,
        'contentType': 'markdown',
        'storagePath': None,
        'bodyMd': '# Math Natural — High-Yield Formula Sheet\n\n### Derivative Rules:\n- Product Rule: (fg)\' = f\'g + fg\'\n- Quotient Rule: (f/g)\' = (f\'g - fg\') / g²\n- Chain Rule: (f(g(x)))\' = f\'(g(x)) · g\'(x)\n\n### Standard Integrals:\n- ∫ 1/x dx = ln|x| + C\n- ∫ e^(kx) dx = (1/k)e^(kx) + C',
        'metaJson': json.dumps({'type': 'cheatsheet'}),
        'published': True
    },
    # Freshman Physics
    {
        'id': 'fn-phys-notes-1',
        'packageId': 'freshman',
        'scopePath': 'freshman/physics',
        'hub': 'short-notes',
        'title': 'Vectors and Two-Dimensional Motion Summary',
        'chapter': 1,
        'sortOrder': 1,
        'contentType': 'markdown',
        'storagePath': None,
        'bodyMd': '# Chapter 1: Vectors & 2D Kinematics\n\n## Vector Components\nFor a vector A at angle θ:\n- $A_x = A \cos(\\theta)$\n- $A_y = A \sin(\\theta)$\n- Magnitude: $|A| = \sqrt{A_x^2 + A_y^2}$\n\n## Projectile Motion Equations\n- Horizontal: $x = (v_0 \cos\\theta) t$\n- Vertical: $y = (v_0 \sin\\theta) t - \\frac{1}{2} g t^2$',
        'metaJson': json.dumps({'author': 'Wisdom Tower Faculty', 'readMinutes': 6}),
        'published': True
    },
    {
        'id': 'fn-phys-flashcards-1',
        'packageId': 'freshman',
        'scopePath': 'freshman/physics',
        'hub': 'flashcards',
        'title': 'Physics Mechanics & Newton Laws Flashcards',
        'chapter': 1,
        'sortOrder': 1,
        'contentType': 'flashcard_deck',
        'storagePath': None,
        'bodyMd': None,
        'metaJson': json.dumps({
            'cards': [
                {'front': 'State Newton\'s Second Law of Motion', 'back': 'The rate of change of momentum of a body is directly proportional to the applied net force and occurs in the direction of the force: F_net = m*a.'},
                {'front': 'What is the Work-Energy Theorem?', 'back': 'The net work done on an object by all forces equals the change in its kinetic energy: W_net = ΔK.'},
                {'front': 'Is gravitational force conservative or non-conservative?', 'back': 'Conservative (work done depends only on initial and final positions, not on path taken).'}
            ]
        }),
        'published': True
    },
    # ECE Electromagnetic Fields (ECEg3051)
    {
        'id': 'ece-3051-notes-1',
        'packageId': 'ece-y3',
        'scopePath': 'ece/sem-1/eceg3051',
        'hub': 'short-notes',
        'title': 'Electrostatics & Gauss\'s Law Fundamentals',
        'chapter': 1,
        'sortOrder': 1,
        'contentType': 'markdown',
        'storagePath': None,
        'bodyMd': '# ECEg3051: Electromagnetic Fields\n\n## Chapter 1: Static Electric Fields\n- **Coulomb\'s Law:** $F = \\frac{q_1 q_2}{4 \\pi \\varepsilon_0 r^2}$\n- **Electric Flux Density (D):** $D = \\varepsilon E$\n- **Gauss\'s Law in Differential Form:** $\\nabla \\cdot D = \\rho_v$\n- **Poisson\'s Equation:** $\\nabla^2 V = -\\frac{\\rho_v}{\\varepsilon}$',
        'metaJson': json.dumps({'author': 'ECE Department Faculty', 'readMinutes': 10}),
        'published': True
    },
    {
        'id': 'ece-3051-flashcards-1',
        'packageId': 'ece-y3',
        'scopePath': 'ece/sem-1/eceg3051',
        'hub': 'flashcards',
        'title': 'Maxwell\'s Equations Fast Recall',
        'chapter': 1,
        'sortOrder': 1,
        'contentType': 'flashcard_deck',
        'storagePath': None,
        'bodyMd': None,
        'metaJson': json.dumps({
            'cards': [
                {'front': 'What is Gauss\'s Law for Magnetism?', 'back': 'Div(B) = 0 (No magnetic monopoles exist).'},
                {'front': 'What is Faraday\'s Law of Induction in point form?', 'back': 'Curl(E) = - ∂B/∂t'},
                {'front': 'What is Ampere-Maxwell Law in differential form?', 'back': 'Curl(H) = J + ∂D/∂t (including displacement current density).'}
            ]
        }),
        'published': True
    },
    # Grade 12 Math
    {
        'id': 'g12-math-notes-1',
        'packageId': 'grade-12',
        'scopePath': 'grade/12/mathematics',
        'hub': 'short-notes',
        'title': 'Chapter 1: Sequences and Series Comprehensive Summary',
        'chapter': 1,
        'sortOrder': 1,
        'contentType': 'markdown',
        'storagePath': None,
        'bodyMd': '# Grade 12 Mathematics: Sequences & Series\n\n## 1.1 Arithmetic Progression (AP)\n- General term: $a_n = a_1 + (n-1)d$\n- Sum of first n terms: $S_n = \\frac{n}{2}[2a_1 + (n-1)d] = \\frac{n}{2}(a_1 + a_n)$\n\n## 1.2 Geometric Progression (GP)\n- General term: $a_n = a_1 r^{n-1}$\n- Sum of finite GP: $S_n = \\frac{a_1(1 - r^n)}{1 - r}$ for $r \\neq 1$\n- Infinite geometric series ($|r| < 1$): $S_\\infty = \\frac{a_1}{1 - r}$',
        'metaJson': json.dumps({'author': 'Wisdom Tower Faculty', 'readMinutes': 7}),
        'published': True
    },
    {
        'id': 'g12-math-flashcards-1',
        'packageId': 'grade-12',
        'scopePath': 'grade/12/mathematics',
        'hub': 'flashcards',
        'title': 'Matriculation Formula Flashcards',
        'chapter': 1,
        'sortOrder': 1,
        'contentType': 'flashcard_deck',
        'storagePath': None,
        'bodyMd': None,
        'metaJson': json.dumps({
            'cards': [
                {'front': 'Formula for sum of infinite geometric series when |r| < 1?', 'back': 'S_inf = a / (1 - r)'},
                {'front': 'Condition for convergence of sequence {r^n}?', 'back': '-1 < r <= 1'},
                {'front': 'Derivative of ln(x)?', 'back': '1/x (for x > 0)'}
            ]
        }),
        'published': True
    }
]

data = {
    'version': 1,
    'generatedAt': '2026-10-10T09:00:00Z',
    'packages': packages,
    'courses': courses,
    'learning_resources': learning_resources
}

with open('app/src/main/assets/data/catalog_snapshot.json', 'w') as f:
    json.dump(data, f, indent=2)

print(f'Catalog snapshot generated! Packages: {len(packages)}, Courses: {len(courses)}, Resources: {len(learning_resources)}')
