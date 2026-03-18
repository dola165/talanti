package ge.dola.talanti.tryout;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Crucial for RLS context execution
public class TryoutService {

    private final TryoutRepository tryoutRepository;

}