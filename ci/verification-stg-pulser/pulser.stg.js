work = load("pulser.stg.work");
write(
    "Combined check: " + checkStgCombined(work) + "\n" +
    "Consistency: " + checkStgConsistency(work) + "\n" +
    "Deadlock freeness: " + checkStgDeadlockFreeness(work) + "\n" +
    "Input properness: " + checkStgInputProperness(work) + "\n" +
    "Output persistency: " + checkStgOutputPersistency(work) + "\n" +
    "Output determinacy: " + checkStgOutputDeterminacy(work) + "\n" +
    "CSC: " + checkStgCsc(work) + "\n" +
    "USC: " + checkStgUsc(work) + "\n" +
    "Absence of self-triggering local signals: " + checkStgLocalSelfTriggering(work) + "\n" +
    "DI interface: " + checkStgDiInterface(work) + "\n" +
    "Normalcy: " + checkStgNormalcy(work) + "\n" +
    "Mutex implementability: " + checkStgMutexImplementability(work) + "\n" +
    "", "pulser.stg.result");
exit();
