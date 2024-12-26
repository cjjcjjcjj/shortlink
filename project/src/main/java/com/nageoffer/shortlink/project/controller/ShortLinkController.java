package com.nageoffer.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shortlink.project.common.convention.result.Result;
import com.nageoffer.shortlink.project.common.convention.result.Results;
import com.nageoffer.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shortlink.project.service.ShortLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 短链接中台控制层
 */
@RestController
@RequiredArgsConstructor// lombok
@RequestMapping("/api/short-link/v1")
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    /**
     * 创建短链接
     * @param shortLinkCreateReqDTO
     * @return
     */
    @PostMapping("/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO shortLinkCreateReqDTO){
        return Results.success(shortLinkService.createShortLink(shortLinkCreateReqDTO));
    }

    /**
     * 修改短链接
     * @param shortLinkUpdateReqDTO
     * @return
     */
    @PostMapping("/update")
    public Result<Void> updataShortLink(ShortLinkUpdateReqDTO shortLinkUpdateReqDTO){
        shortLinkService.updateShortLink(shortLinkUpdateReqDTO);
        return Results.success();
    }

    /**
     * 短链接分页查询
     * @param shortLinkPageReqDTO
     * @return
     */
    @GetMapping("/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO shortLinkPageReqDTO) {
        return Results.success(shortLinkService.pageShortLink(shortLinkPageReqDTO));
    }


    /**
     * 查询短链接分组内数量
     * @return
     */
    @GetMapping("/count")
    public Result<List<ShortLinkGroupCountQueryRespDTO>> listGroupShortLinkCount(@RequestParam List<String> gids){
        return Results.success(shortLinkService.listGroupShortLinkCount(gids));
    }


}