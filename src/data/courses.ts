import { BookModule, CoursePackage, Flashcard, NotificationItem } from '../types';

export const PRESEEDED_BOOK_SIZES: Record<string, number> = {
  "6aad8e8e001bf655ddff": 1706717, // Anthropology module (1.6 MB)
  "6aad8e8e001c0003600a": 1891010, // Communicative English 1 (1.8 MB)
  "6aad8e8e001c0c928f44": 5734887, // Math natural , module (5.5 MB)
  "6aad8e8e001c0c84652a": 2415338, // Physical fitness module (2.3 MB)
  "6aad8e8e001c09f7da8a": 4768641, // Physics module (4.5 MB)
  "6aad8e8e001c0262c6cb": 2753014, // Logic module (2.6 MB)
  "6aad8e8e001c0725f10e": 1882181, // Economics module (1.8 MB)
  "6aad8e8e001c047af478": 2456883, // Entrepreneurship module (2.3 MB)
  "6aad8e8e001c0c362ecf": 3285055, // History module (3.1 MB)
  "6aad8e8e001c0a2d2554": 9492546, // Chemistry module (9.1 MB)
  "6aad8e8e001c0c640e4e": 2347872, // C++ material (2.2 MB)
  "6aad8e8e001c04690369": 786264,  // Communicative English 2 (768 KB)
  "6aad8e8e001c0c040515": 5016415, // Math social, module (4.8 MB)
  "6aad8e8e001c0c6d982f": 1906455, // Psychology module (1.8 MB)
  "6aad8e8e001c054431c6": 3089018, // Geography module (2.9 MB)
  "6aad8e8e001c015e87f0": 2298390, // Civic module (2.2 MB)
  "6aad8e8e001c0aafc2d6": 2717979, // Emerging module (2.6 MB)
  "6aad8e8e001c02e0ae41": 1973616, // Global module (1.9 MB)
  "6aad8e8e001c095bdf35": 2604265, // Inclusiveness module (2.5 MB)
  "6aad8e8e001c036a8350": 5309172, // Biology module (5.1 MB)
  "6aad8e8e001c035bd707": 2238923, // Applied math 1 (2.1 MB)
  "6aad97eb0010d21989e8": 8167554, // Digital logic design (7.8 MB)
  "6aad97eb0010ea179f50": 2438550, // Thermodynamics material (2.3 MB)
  "6aa99489001c02f4b410": 8931163, // Physics Grade 9 (8.5 MB)
  "6aad97eb0010d2e5f102": 9073900, // Introduction to machines PDF (8.7 MB)
  "6aad97eb0010edc8670d": 6901577, // Network analysis and synthesis (6.6 MB)
  "6aad97eb0010e4ee5f12": 5917129  // Material pdf (5.6 MB)
};

export const ALL_BOOK_MODULES: BookModule[] = [
  {
    id: "anthro-freshman",
    fileId: "6aad8e8e001bf655ddff",
    title: "Anthropology Module",
    category: "Freshman",
    sizeBytes: 1706717,
    pagesCount: 164,
    authorOrDept: "Ministry of Education (MoE) Ethiopia",
    description: "General Anthropology module for Ethiopian higher education institutions covering cultural diversity, kinship, and human evolution.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001bf655ddff",
    sampleContent: [
      {
        chapter: "Chapter 1: Introducing Anthropology and Its Sub-fields",
        sections: [
          { heading: "1.1 What is Anthropology?", body: "Anthropology is the scientific and holistic study of humanity across all places and periods of time. It encompasses both physical and cultural dimensions of humanity." },
          { heading: "1.2 The Four Sub-disciplines", body: "Physical anthropology, archaeology, linguistic anthropology, and socio-cultural anthropology form the four-field approach in Ethiopia's tertiary curriculum." }
        ]
      },
      {
        chapter: "Chapter 2: Human Culture and Society",
        sections: [
          { heading: "2.1 The Concept of Culture", body: "Culture is learned, shared, symbolic, integrated, and dynamic. Culture provides meaning to human practices in diverse Ethiopian communities." }
        ]
      }
    ]
  },
  {
    id: "english-1-freshman",
    fileId: "6aad8e8e001c0003600a",
    title: "Communicative English Language Skills I",
    category: "Freshman",
    sizeBytes: 1891010,
    pagesCount: 178,
    authorOrDept: "Department of English Language & Literature",
    description: "Core academic communicative English for first-year university students in Ethiopian universities.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c0003600a",
    sampleContent: [
      {
        chapter: "Unit 1: Study Skills and Reading Comprehension",
        sections: [
          { heading: "Active Reading", body: "Techniques for academic skimming, scanning, in-depth reading, and summarizing textbook passages effectively." },
          { heading: "Vocabulary Building", body: "Identifying word context, roots, prefixes, suffixes, and contextual guessing in academic papers." }
        ]
      }
    ]
  },
  {
    id: "math-natural-freshman",
    fileId: "6aad8e8e001c0c928f44",
    title: "Mathematics for Natural Sciences",
    category: "Natural Science",
    sizeBytes: 5734887,
    pagesCount: 312,
    authorOrDept: "MoE Department of Mathematics",
    description: "Rigorous freshman calculus, linear algebra, limits, differentiation, and integration for natural science and engineering students.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c0c928f44",
    sampleContent: [
      {
        chapter: "Chapter 1: Propositional Logic and Set Theory",
        sections: [
          { heading: "1.1 Propositions and Logical Connectives", body: "Truth tables, conditional and biconditional statements, tautology, contradiction, and logical equivalences." },
          { heading: "1.2 Quantifiers and Mathematical Induction", body: "Universal and existential quantifiers; proving properties over natural numbers using mathematical induction." }
        ]
      },
      {
        chapter: "Chapter 2: Functions, Limits and Continuity",
        sections: [
          { heading: "2.1 Precise Definition of Limit", body: "Epsilon-delta definitions, limit laws, algebraic computation of limits, and one-sided limits." }
        ]
      }
    ]
  },
  {
    id: "physics-module-freshman",
    fileId: "6aad8e8e001c09f7da8a",
    title: "General Physics Module",
    category: "Natural Science",
    sizeBytes: 4768641,
    pagesCount: 286,
    authorOrDept: "Department of Physics, AAU",
    description: "Mechanics, thermodynamics, wave mechanics, electromagnetism, and modern physics foundations with solved exam problems.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c09f7da8a",
    sampleContent: [
      {
        chapter: "Chapter 1: Vectors and Kinematics",
        sections: [
          { heading: "Vector Operations", body: "Dot products, cross products, coordinate transformations, projectile motion, and circular motion vectors." }
        ]
      }
    ]
  },
  {
    id: "logic-freshman",
    fileId: "6aad8e8e001c0262c6cb",
    title: "Logic and Critical Thinking",
    category: "Freshman",
    sizeBytes: 2753014,
    pagesCount: 195,
    authorOrDept: "Department of Philosophy",
    description: "Formal and informal fallacies, syllogistic reasoning, argument evaluation, and cognitive bias analysis for academic rigor.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c0262c6cb",
    sampleContent: [
      {
        chapter: "Chapter 1: Logic and Arguments",
        sections: [
          { heading: "Anatomy of an Argument", body: "Premises, conclusions, indicators, deductive vs inductive arguments, validity, soundness, and strength." }
        ]
      }
    ]
  },
  {
    id: "economics-freshman",
    fileId: "6aad8e8e001c0725f10e",
    title: "Economics Module",
    category: "Social Science",
    sizeBytes: 1882181,
    pagesCount: 182,
    authorOrDept: "Department of Economics",
    description: "Microeconomics principles, elasticity, consumer choice, market structures, and macro fundamentals for Ethiopian development.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c0725f10e",
    sampleContent: [
      {
        chapter: "Chapter 1: Theory of Consumer Behavior",
        sections: [
          { heading: "Cardinal vs Ordinal Utility", body: "Diminishing marginal utility, indifference curves, budget constraints, and consumer equilibrium." }
        ]
      }
    ]
  },
  {
    id: "history-freshman",
    fileId: "6aad8e8e001c0c362ecf",
    title: "History of Ethiopia and the Horn",
    category: "Freshman",
    sizeBytes: 3285055,
    pagesCount: 220,
    authorOrDept: "MoE History Panel",
    description: "Comprehensive history of Ethiopia and the Horn from prehistoric origins to the late twentieth century.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c0c362ecf",
    sampleContent: [
      {
        chapter: "Chapter 1: Introduction to History & Historiography",
        sections: [
          { heading: "Nature and Value of History", body: "Primary and secondary sources, oral tradition, paleography, and archaeological evidence in Ethiopian history." }
        ]
      }
    ]
  },
  {
    id: "chemistry-freshman",
    fileId: "6aad8e8e001c0a2d2554",
    title: "General Chemistry Module",
    category: "Natural Science",
    sizeBytes: 9492546,
    pagesCount: 390,
    authorOrDept: "Department of Chemistry",
    description: "Atomic structure, chemical bonding, stoichiometry, equilibrium, thermodynamics, and kinetics for university freshman sciences.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c0a2d2554",
    sampleContent: [
      {
        chapter: "Chapter 1: Atomic Structure and Quantum Mechanics",
        sections: [
          { heading: "Quantum Numbers and Orbitals", body: "Schrodinger wave equations, Pauli exclusion principle, Hund's rule, and electronic configurations." }
        ]
      }
    ]
  },
  {
    id: "cpp-freshman",
    fileId: "6aad8e8e001c0c640e4e",
    title: "Introduction to C++ Programming",
    category: "Engineering",
    sizeBytes: 2347872,
    pagesCount: 180,
    authorOrDept: "Faculty of Computing & Informatics",
    description: "Object-oriented programming, data structures, memory management, pointers, and algorithmic problem solving in C++.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c0c640e4e",
    sampleContent: [
      {
        chapter: "Chapter 1: Basics of C++ and Memory",
        sections: [
          { heading: "Pointers and Dynamic Allocation", body: "Memory addresses, pointer arithmetic, new and delete operators, avoiding memory leaks." }
        ]
      }
    ]
  },
  {
    id: "digital-logic",
    fileId: "6aad97eb0010d21989e8",
    title: "Digital Logic Design",
    category: "Engineering",
    sizeBytes: 8167554,
    pagesCount: 340,
    authorOrDept: "Electrical and Computer Engineering",
    description: "Boolean algebra, Karnaugh maps, combinational circuit synthesis, flip-flops, sequential circuits, and state machines.",
    downloadUrl: "/api/content/pdf?id=6aad97eb0010d21989e8",
    sampleContent: [
      {
        chapter: "Chapter 1: Boolean Algebra & Logic Gates",
        sections: [
          { heading: "Minimization with K-Maps", body: "2, 3, and 4 variable Karnaugh maps, don't-care conditions, and hazard-free circuit synthesis." }
        ]
      }
    ]
  },
  {
    id: "emerging-tech",
    fileId: "6aad8e8e001c0aafc2d6",
    title: "Emerging Technologies Module",
    category: "Freshman",
    sizeBytes: 2717979,
    pagesCount: 198,
    authorOrDept: "MoE Innovation & Technology Panel",
    description: "Artificial Intelligence, Internet of Things (IoT), Blockchain, Cloud Computing, Cybersecurity, and Data Science fundamentals.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c0aafc2d6",
    sampleContent: [
      {
        chapter: "Chapter 1: Introduction to Industry 4.0 & AI",
        sections: [
          { heading: "Core AI Paradigms", body: "Machine learning, neural networks, supervised vs unsupervised models, and generative intelligence applications." }
        ]
      }
    ]
  },
  {
    id: "psychology-freshman",
    fileId: "6aad8e8e001c0c6d982f",
    title: "General Psychology Module",
    category: "Freshman",
    sizeBytes: 1906455,
    pagesCount: 168,
    authorOrDept: "Department of Psychology",
    description: "Human development, cognitive processes, personality theories, motivation, learning principles, and mental well-being.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c0c6d982f",
    sampleContent: [
      {
        chapter: "Chapter 1: The Science of Psychology",
        sections: [
          { heading: "Foundations & Perspectives", body: "Biological, cognitive, psychodynamic, behavioral, and socio-cultural lenses of understanding mind and action." }
        ]
      }
    ]
  },
  {
    id: "physics-grade9",
    fileId: "6aa99489001c02f4b410",
    title: "Physics Grade 9 Textbook & Module",
    category: "Grade 9-12",
    sizeBytes: 8931163,
    pagesCount: 320,
    authorOrDept: "Ethiopian Curriculum Development Agency",
    description: "Complete Grade 9 Physics curriculum textbook with worked examples, review exercises, and national exam prep questions.",
    downloadUrl: "/api/content/pdf?id=6aa99489001c02f4b410",
    sampleContent: [
      {
        chapter: "Chapter 1: Physics and the Human Society",
        sections: [
          { heading: "Measurements and Units", body: "SI base units, derived units, significant figures, and scientific notation in physical measurements." }
        ]
      }
    ]
  },
  {
    id: "math-social-freshman",
    fileId: "6aad8e8e001c0c040515",
    title: "Mathematics for Social Sciences",
    category: "Social Science",
    sizeBytes: 5016415,
    pagesCount: 260,
    authorOrDept: "MoE Social Sciences Mathematics Board",
    description: "Matrix algebra, linear programming, financial mathematics, statistics, and business calculus for social science disciplines.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c0c040515",
    sampleContent: [
      {
        chapter: "Chapter 1: Matrix Algebra and Linear Systems",
        sections: [
          { heading: "Gaussian Elimination & Inversion", body: "Solving systems of linear equations in economics and management using elementary row operations." }
        ]
      }
    ]
  },
  {
    id: "civics-freshman",
    fileId: "6aad8e8e001c015e87f0",
    title: "Moral and Civic Education",
    category: "Freshman",
    sizeBytes: 2298390,
    pagesCount: 174,
    authorOrDept: "Department of Civic and Ethical Studies",
    description: "Ethics, constitution, human rights, rule of law, citizenship responsibilities, and democratic principles in the Horn of Africa.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c015e87f0",
    sampleContent: [
      {
        chapter: "Chapter 1: Ethics and Morality",
        sections: [
          { heading: "Ethical Theories", body: "Teleological, deontological, and virtue ethics perspectives applied to public service and citizenship." }
        ]
      }
    ]
  },
  {
    id: "geography-freshman",
    fileId: "6aad8e8e001c054431c6",
    title: "Geography of Ethiopia and the Horn",
    category: "Freshman",
    sizeBytes: 3089018,
    pagesCount: 215,
    authorOrDept: "Department of Geography & Environmental Studies",
    description: "Physical geography, geology, drainage systems, climate zones, population demographics, and economic development of Ethiopia.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c054431c6",
    sampleContent: [
      {
        chapter: "Chapter 1: Physiography and Relief of Ethiopia",
        sections: [
          { heading: "The Ethiopian Rift System", body: "Tectonic formation, Western Highlands, Southeastern Highlands, and lowlands morphology." }
        ]
      }
    ]
  },
  {
    id: "inclusiveness-freshman",
    fileId: "6aad8e8e001c095bdf35",
    title: "Inclusiveness Module",
    category: "Freshman",
    sizeBytes: 2604265,
    pagesCount: 185,
    authorOrDept: "MoE Special Needs & Inclusive Education",
    description: "Understanding disabilities, barriers, legal frameworks, and creating universal accessibility in Ethiopian schools and workplaces.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c095bdf35",
    sampleContent: [
      {
        chapter: "Chapter 1: Understanding Inclusion & Diversity",
        sections: [
          { heading: "Models of Disability", body: "Medical vs social models of disability; rights-based empowerment and structural accommodation." }
        ]
      }
    ]
  },
  {
    id: "global-trends",
    fileId: "6aad8e8e001c02e0ae41",
    title: "Global Trends and Affairs",
    category: "Freshman",
    sizeBytes: 1973616,
    pagesCount: 160,
    authorOrDept: "Department of Political Science & International Relations",
    description: "International relations, globalization, foreign policy of Ethiopia, regional integration in East Africa (IGAD/AU), and geopolitics.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c02e0ae41",
    sampleContent: [
      {
        chapter: "Chapter 1: Theories of International Relations",
        sections: [
          { heading: "Realism vs Liberalism", body: "State sovereignty, power distribution, international treaties, and multilateral cooperation." }
        ]
      }
    ]
  },
  {
    id: "entrepreneurship-freshman",
    fileId: "6aad8e8e001c047af478",
    title: "Entrepreneurship Module",
    category: "Freshman",
    sizeBytes: 2456883,
    pagesCount: 190,
    authorOrDept: "School of Commerce",
    description: "Business planning, venture creation, market feasibility, financial projections, and SME incubation tailored to Ethiopia.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c047af478",
    sampleContent: [
      {
        chapter: "Chapter 1: The Entrepreneurial Mindset",
        sections: [
          { heading: "Opportunity Recognition", body: "Identifying market pain points, value proposition design, and business model canvas creation." }
        ]
      }
    ]
  },
  {
    id: "physical-fitness",
    fileId: "6aad8e8e001c0c84652a",
    title: "Physical Fitness and Conditioning",
    category: "Freshman",
    sizeBytes: 2415338,
    pagesCount: 154,
    authorOrDept: "Department of Sport Sciences",
    description: "Principles of physical fitness, cardiovascular health, nutrition, biomechanics of exercise, and lifetime wellness.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c0c84652a",
    sampleContent: [
      {
        chapter: "Chapter 1: Components of Physical Fitness",
        sections: [
          { heading: "Health-Related Fitness", body: "Cardiorespiratory endurance, muscular strength, flexibility, and body composition indices." }
        ]
      }
    ]
  },
  {
    id: "biology-freshman",
    fileId: "6aad8e8e001c036a8350",
    title: "General Biology Module",
    category: "Natural Science",
    sizeBytes: 5309172,
    pagesCount: 298,
    authorOrDept: "Department of Biology & Biotechnology",
    description: "Cellular biology, genetics, evolutionary theory, ecology, and plant/animal physiology for freshman university students.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c036a8350",
    sampleContent: [
      {
        chapter: "Chapter 1: Cellular Organization & Energy",
        sections: [
          { heading: "Cell Membrane Dynamics", body: "Lipid bilayers, membrane proteins, active and passive transport, osmolarity, and cellular signaling." }
        ]
      }
    ]
  },
  {
    id: "applied-math1",
    fileId: "6aad8e8e001c035bd707",
    title: "Applied Mathematics I",
    category: "Engineering",
    sizeBytes: 2238923,
    pagesCount: 210,
    authorOrDept: "Faculty of Applied Sciences",
    description: "Differential equations, Laplace transforms, Fourier series, and applied numerical approximation methods.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c035bd707",
    sampleContent: [
      {
        chapter: "Chapter 1: First-Order Ordinary Differential Equations",
        sections: [
          { heading: "Separable and Exact Equations", body: "Techniques for integrating factors, initial value problems, and engineering system modeling." }
        ]
      }
    ]
  },
  {
    id: "thermodynamics",
    fileId: "6aad97eb0010ea179f50",
    title: "Thermodynamics Material",
    category: "Engineering",
    sizeBytes: 2438550,
    pagesCount: 230,
    authorOrDept: "Mechanical Engineering Dept",
    description: "First and second laws of thermodynamics, closed and open systems, Carnot cycles, entropy, and heat engines.",
    downloadUrl: "/api/content/pdf?id=6aad97eb0010ea179f50",
    sampleContent: [
      {
        chapter: "Chapter 1: Properties of Pure Substances",
        sections: [
          { heading: "Phase Change and Vapor Tables", body: "Saturated liquid-vapor mixtures, superheated vapor, ideal gas relationships, and enthalpy." }
        ]
      }
    ]
  },
  {
    id: "intro-machines",
    fileId: "6aad97eb0010d2e5f102",
    title: "Introduction to Machines PDF",
    category: "Engineering",
    sizeBytes: 9073900,
    pagesCount: 360,
    authorOrDept: "Faculty of Technology",
    description: "Transformers, DC machines, AC synchronous generators, induction motors, and magnetic circuit principles.",
    downloadUrl: "/api/content/pdf?id=6aad97eb0010d2e5f102",
    sampleContent: [
      {
        chapter: "Chapter 1: Magnetic Circuits & Electromechanical Energy",
        sections: [
          { heading: "Faraday's Law and Magnetic Flux", body: "Reluctance, permeance, hysteresis losses, and torque production principles in rotational machinery." }
        ]
      }
    ]
  },
  {
    id: "network-analysis",
    fileId: "6aad97eb0010edc8670d",
    title: "Network Analysis and Synthesis",
    category: "Engineering",
    sizeBytes: 6901577,
    pagesCount: 280,
    authorOrDept: "Electrical Engineering Panel",
    description: "AC and DC network theorems, two-port networks, resonance, transient analysis, and passive filter design.",
    downloadUrl: "/api/content/pdf?id=6aad97eb0010edc8670d",
    sampleContent: [
      {
        chapter: "Chapter 1: Circuit Theorems for Frequency Domain",
        sections: [
          { heading: "Thevenin, Norton, & Maximum Power", body: "Impedance matching in sinusoidal steady state and complex frequency representation." }
        ]
      }
    ]
  },
  {
    id: "english-2-freshman",
    fileId: "6aad8e8e001c04690369",
    title: "Communicative English Language Skills II",
    category: "Freshman",
    sizeBytes: 786264,
    pagesCount: 140,
    authorOrDept: "Department of English Language & Literature",
    description: "Academic writing, essay composition, research citations (APA/MLA), debating, and oral presentation mastery.",
    downloadUrl: "/api/content/pdf?id=6aad8e8e001c04690369",
    sampleContent: [
      {
        chapter: "Unit 1: The Academic Essay & Argumentation",
        sections: [
          { heading: "Thesis Statements & Cohesion", body: "Developing persuasive thesis statements, transitions, counterarguments, and evidentiary support." }
        ]
      }
    ]
  },
  {
    id: "material-science",
    fileId: "6aad97eb0010e4ee5f12",
    title: "Material Science Engineering PDF",
    category: "Engineering",
    sizeBytes: 5917129,
    pagesCount: 275,
    authorOrDept: "Department of Materials Engineering",
    description: "Crystal structures, atomic imperfections, phase diagrams, mechanical testing, polymers, ceramics, and composites.",
    downloadUrl: "/api/content/pdf?id=6aad97eb0010e4ee5f12",
    sampleContent: [
      {
        chapter: "Chapter 1: Atomic Bonding and Crystal Lattices",
        sections: [
          { heading: "BCC, FCC, and HCP Structures", body: "Atomic packing factors, Miller indices for crystallographic planes and directions." }
        ]
      }
    ]
  }
];

export const FLASHCARDS: Flashcard[] = [
  {
    id: "fc-1",
    subject: "Anthropology",
    question: "What is ethnocentrism in cultural anthropology?",
    answer: "The tendency to judge other cultures by the standards of one's own culture.",
    explanation: "Anthropologists counter ethnocentrism with cultural relativism—evaluating beliefs and behaviors in the context of their own society."
  },
  {
    id: "fc-2",
    subject: "Logic",
    question: "What distinguishes a sound argument from a valid argument?",
    answer: "A sound argument is both deductively valid AND all of its premises are factually true.",
    explanation: "Validity only concerns logical structure (if premises were true, conclusion must follow). Soundness requires factual truth as well."
  },
  {
    id: "fc-3",
    subject: "Natural Mathematics",
    question: "State the Mean Value Theorem (MVT) for a differentiable function f on [a, b].",
    answer: "There exists at least one c in (a, b) such that f'(c) = [f(b) - f(a)] / (b - a).",
    explanation: "Geometrically, this means at some point the tangent slope equals the average secant slope across the interval."
  },
  {
    id: "fc-4",
    subject: "Physics",
    question: "What is the physical significance of the curl of a conservative vector field?",
    answer: "The curl is identically zero (∇ × F = 0), meaning line integrals are path-independent.",
    explanation: "A conservative force field can be expressed as the negative gradient of a potential energy function (F = -∇U)."
  },
  {
    id: "fc-5",
    subject: "Economics",
    question: "What occurs when price elasticity of demand (|Ed|) is strictly greater than 1?",
    answer: "Demand is price elastic: a decrease in price leads to an increase in total revenue.",
    explanation: "Percentage change in quantity demanded exceeds percentage change in price, so volume gains outweigh unit price reductions."
  },
  {
    id: "fc-6",
    subject: "Emerging Tech",
    question: "What is the consensus mechanism in public Proof-of-Work blockchain systems?",
    answer: "Nodes solve computationally intensive cryptographic puzzles to validate blocks and earn rewards.",
    explanation: "Pioneered by Bitcoin, it provides Sybil attack resistance and Byzantine fault tolerance without a central authority."
  },
  {
    id: "fc-7",
    subject: "C++ Programming",
    question: "What is the difference between shallow copy and deep copy in C++?",
    answer: "Shallow copy duplicates pointer addresses; deep copy allocates new memory and duplicates the underlying data.",
    explanation: "Shallow copies with dynamic memory can cause double-free errors when destructors are invoked."
  }
];

export const COURSE_PACKAGES: CoursePackage[] = [
  {
    id: "freshman-complete",
    title: "University Freshman Complete Suite",
    tagline: "The all-inclusive package for first-year students in Ethiopian public & private universities.",
    priceETB: 1200,
    originalPriceETB: 2400,
    badge: "MOST POPULAR",
    category: "University",
    enrolledStudents: 14200,
    rating: 4.9,
    features: [
      "All 12 Freshman Semester 1 & 2 Modules (PDFs + Audio summary)",
      "Zero-data Offline Vault caching for unlimited offline reading",
      "Over 1,200 Interactive Flashcards with 3D Flip & Spaced Repetition",
      "Past Midterm & Final Exam Solutions from AAU, ASTU, Hawassa, & BD University",
      "AI Study Companion with step-by-step problem breakdown"
    ]
  },
  {
    id: "natural-science-intensive",
    title: "Natural Sciences & Pre-Engineering Mastery",
    tagline: "Calculus, General Physics, Chemistry, and C++ rigorous exam preparation.",
    priceETB: 950,
    originalPriceETB: 1800,
    badge: "ACCURACY VERIFIED",
    category: "Natural Science",
    enrolledStudents: 9800,
    rating: 4.95,
    features: [
      "In-depth video-style formula step-by-steps and derivations",
      "Complete textbook problem sets with Ethiopian professor annotations",
      "Digital Logic, Applied Math, and C++ practical lab code files",
      "Unlimited mock examinations with automated timer & score analytics"
    ]
  },
  {
    id: "remedial-package",
    title: "National Remedial Program Intensive",
    tagline: "Guaranteed pathway to university admission with focused national curriculum review.",
    priceETB: 800,
    originalPriceETB: 1500,
    category: "Remedial",
    enrolledStudents: 6400,
    rating: 4.85,
    features: [
      "Targeted remedial syllabus coverage for Natural & Social tracks",
      "High-frequency national exam questions from past 7 years",
      "Full offline textbook vault downloads",
      "Weekly diagnostic practice assessments with personalized weakness review"
    ]
  },
  {
    id: "stem-highschool",
    title: "Grade 9–12 STEM & National Examination Prep",
    tagline: "Build unshakeable foundations in Physics, Chemistry, Math, and Biology for Grade 12 UEE.",
    priceETB: 850,
    originalPriceETB: 1600,
    category: "High School",
    enrolledStudents: 18500,
    rating: 4.92,
    features: [
      "Curriculum aligned with newest Ethiopian Educational Roadmap",
      "Grade 9 through 12 complete textbook library & question banks",
      "Over 2,500 multiple-choice questions with thorough justifications",
      "Study planner schedule and streak rewards"
    ]
  }
];

export const INITIAL_NOTIFICATIONS: NotificationItem[] = [
  {
    id: "notif-1",
    title: "New Freshman Modules Added to Vault",
    message: "Anthropology, Logic, and Mathematics for Natural Sciences have been updated with 2026 semester exam hints. Tap to open and cache.",
    timestamp: "10 mins ago",
    type: "material",
    read: false,
    link: "/learning"
  },
  {
    id: "notif-2",
    title: "Package Enrollment Active",
    message: "Your subscription to University Freshman Complete Suite is verified and active. All 27 books are unlocked in the Vault.",
    timestamp: "2 hours ago",
    type: "order",
    read: false,
    link: "/account"
  },
  {
    id: "notif-3",
    title: "National Exam Schedule Released",
    message: "MoE has announced key milestone dates for Ethiopian University Entrance & Freshman Midterm schedules.",
    timestamp: "Yesterday",
    type: "exam",
    read: true,
    link: "/notifications"
  },
  {
    id: "notif-4",
    title: "Offline Vault Sync Ready",
    message: "Save your favorite course modules while connected to Wi-Fi to study anywhere across Ethiopia without consuming mobile data.",
    timestamp: "3 days ago",
    type: "system",
    read: true,
    link: "/settings"
  }
];
