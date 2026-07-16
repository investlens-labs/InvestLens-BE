package com.investlens.portfolio.presentation;

import com.investlens.common.error.BusinessException;
import com.investlens.common.error.ErrorCode;
import com.investlens.portfolio.application.PortfolioService;
import com.investlens.portfolio.presentation.dto.AddPortfolioRequest;
import com.investlens.portfolio.presentation.dto.PortfolioItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolio")
@Tag(name = "Portfolio", description = "인증 사용자의 보유·관심 종목 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class PortfolioController {
    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    @Operation(summary = "내 포트폴리오 조회")
    public List<PortfolioItemResponse> getPortfolio(Authentication authentication) {
        return portfolioService.getPortfolio(currentUserId(authentication));
    }

    @PostMapping
    @Operation(summary = "포트폴리오 종목 등록")
    public ResponseEntity<PortfolioItemResponse> add(
            Authentication authentication,
            @Valid @RequestBody AddPortfolioRequest request
    ) {
        PortfolioItemResponse response = portfolioService.add(currentUserId(authentication), request.instrumentId());
        return ResponseEntity.created(URI.create("/api/v1/portfolio/" + response.id())).body(response);
    }

    @DeleteMapping("/{portfolioItemId}")
    @Operation(summary = "포트폴리오 종목 삭제", description = "본인 포트폴리오에 등록된 항목만 삭제할 수 있습니다.")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID portfolioItemId) {
        portfolioService.delete(currentUserId(authentication), portfolioItemId);
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId(Authentication authentication) {
        try {
            return UUID.fromString(authentication.getName());
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }
}
