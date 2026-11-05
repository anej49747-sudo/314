package csit314.renovation.discovery.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** JDBC boundary to the additive V19 tables. No seed data is required. */
@Service
public class DiscoveryService {
    // Matches PostgreSQL NUMERIC(10,2) and the existing designer form validation.
    private static final BigDecimal MAX_FLOOR_AREA_SQM = new BigDecimal("99999999.99");
    private final NamedParameterJdbcTemplate db;
    private final UserAccountRepository accounts;
    public DiscoveryService(NamedParameterJdbcTemplate db, UserAccountRepository accounts) {
        this.db=db; this.accounts=accounts;
    }
    private Map<String,Object> args(Object... values) {
        Map<String,Object> m=new HashMap<>();
        for(int i=0;i<values.length;i+=2) m.put((String)values[i],values[i+1]);
        return m;
    }
    private int update(String sql, Map<String,?> params) { return db.update(sql, params); }
    private List<Map<String,Object>> rows(String sql, Map<String,?> params) { return db.queryForList(sql,params); }
    private long count(String sql, Map<String,?> params) { return db.queryForObject(sql,params,Long.class); }
    private UserAccount signedIn(Authentication auth, UserRole role) {
        if(auth==null || !auth.isAuthenticated()) throw new AccessDeniedException("Login required");
        UserAccount user=accounts.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(()->new AccessDeniedException("Login required"));
        if(!user.isActive() || user.getRole()!=role) throw new AccessDeniedException("Incorrect account role");
        return user;
    }
    private Long optionalCustomer(Authentication auth) {
        if(auth==null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return null;
        return accounts.findByEmailIgnoreCase(auth.getName())
                .filter(a->a.isActive() && a.getRole()==UserRole.CUSTOMER).map(UserAccount::getId).orElse(null);
    }
    private String table(String kind) {
        return switch(kind) { case "categories" -> "project_categories"; case "styles" -> "design_styles";
            case "property-types" -> "property_types"; case "size-ranges" -> "size_ranges";
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Unknown catalogue"); };
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> catalogue(String kind) {
        String t=table(kind);
        return rows("SELECT * FROM "+t+" ORDER BY name,id",Map.of());
    }
    private String clean(String name) {
        if(name==null || name.strip().isBlank() || name.strip().length()>100) throw new IllegalArgumentException("Name must be 1–100 characters");
        return name.strip();
    }
    private void checkRange(BigDecimal min, BigDecimal max) {
        if(min!=null && min.signum()<0 || max!=null && max.signum()<=0 || min!=null && max!=null && min.compareTo(max)>=0)
            throw new IllegalArgumentException("Choose a positive, non-empty size interval");
    }
    @Transactional
    public Map<String,Object> saveCatalogue(String kind, Long id, CatalogueInput input, Authentication auth) {
        signedIn(auth,UserRole.PLATFORM_MANAGER);
        String table=table(kind); String name=clean(input.name());
        if("size-ranges".equals(kind)) checkRange(input.minSqm(),input.maxSqm());
        var params=args("name",name,"id",id,"min",input.minSqm(),"max",input.maxSqm());
        if(id!=null && count("SELECT count(*) FROM "+table+" WHERE id=:id",params)==0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Classification not found");
        try {
            if("size-ranges".equals(kind)) {
                if(id==null) id=db.queryForObject("INSERT INTO size_ranges(name,min_sqm,max_sqm) VALUES(:name,:min,:max) RETURNING id",params,Long.class);
                else update("UPDATE size_ranges SET name=:name,min_sqm=:min,max_sqm=:max WHERE id=:id",params);
            } else {
                if(id==null) id=db.queryForObject("INSERT INTO "+table+"(name) VALUES(:name) RETURNING id",params,Long.class);
                else update("UPDATE "+table+" SET name=:name WHERE id=:id",params);
            }
        } catch(org.springframework.dao.DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Duplicate name or overlapping size interval",e);
        }
        return rows("SELECT * FROM "+table+" WHERE id=:id",args("id",id)).get(0);
    }
    @Transactional
    public void deleteCatalogue(String kind,Long id, Authentication auth) {
        signedIn(auth,UserRole.PLATFORM_MANAGER);
        if(update("DELETE FROM "+table(kind)+" WHERE id=:id",args("id",id))==0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Classification not found");
    }
    private void requireIds(String table,Collection<Long> ids) {
        if(ids==null || ids.isEmpty()) return;
        if(ids.stream().anyMatch(Objects::isNull) || ids.size()!=new HashSet<>(ids).size()) throw new IllegalArgumentException("Duplicate or invalid selections");
        long found=count("SELECT count(*) FROM "+table+" WHERE id IN (:ids)",args("ids",ids));
        if(found!=ids.size()) throw new IllegalArgumentException("Select existing classifications");
    }
    public record CatalogueInput(String name, BigDecimal minSqm, BigDecimal maxSqm) {}
    public record MetadataInput(Long propertyTypeId, BigDecimal floorAreaSqm, Long primaryStyleId, List<Long> secondaryStyleIds) {}
    public record PreferencesInput(List<Long> categoryIds,List<Long> styleIds,List<Long> propertyTypeIds,Long sizeRangeId) {}

    @Transactional(readOnly=true)
    public Map<String,Object> metadata(Long projectId) {
        var list=rows("SELECT m.property_type_id, t.name property_type_name, m.floor_area_sqm FROM project_metadata m LEFT JOIN property_types t ON t.id=m.property_type_id WHERE m.project_id=:id",args("id",projectId));
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("propertyTypeId",list.isEmpty()?null:list.get(0).get("property_type_id"));
        out.put("propertyTypeName",list.isEmpty()?null:list.get(0).get("property_type_name"));
        out.put("floorAreaSqm",list.isEmpty()?null:list.get(0).get("floor_area_sqm"));
        var styles=rows("SELECT s.id,s.name,ps.role FROM project_design_styles ps JOIN design_styles s ON s.id=ps.style_id WHERE ps.project_id=:id ORDER BY CASE WHEN ps.role='PRIMARY' THEN 0 ELSE 1 END,s.name",args("id",projectId));
        out.put("styles",styles); return out;
    }
    /** Read portfolio classifications in two queries, including legacy projects with no V19 metadata. */
    @Transactional(readOnly=true)
    public Map<Long,Map<String,Object>> metadataForProjects(List<Long> projectIds) {
        if(projectIds==null || projectIds.isEmpty()) return Map.of();
        Map<Long,Map<String,Object>> result=new HashMap<>();
        for(Long id:projectIds) {
            Map<String,Object> item=new LinkedHashMap<>();
            item.put("propertyTypeId",null);
            item.put("propertyTypeName",null);
            item.put("floorAreaSqm",null);
            item.put("styles",new ArrayList<Map<String,Object>>());
            result.put(id,item);
        }
        var params=args("ids",projectIds);
        for(var row:rows("SELECT m.project_id, m.property_type_id, t.name property_type_name, m.floor_area_sqm "
                +"FROM project_metadata m LEFT JOIN property_types t ON t.id=m.property_type_id "
                +"WHERE m.project_id IN (:ids)",params)) {
            long id=((Number)row.get("project_id")).longValue();
            var item=result.get(id);
            if(item!=null) {
                item.put("propertyTypeId",row.get("property_type_id"));
                item.put("propertyTypeName",row.get("property_type_name"));
                item.put("floorAreaSqm",row.get("floor_area_sqm"));
            }
        }
        for(var row:rows("SELECT ps.project_id,s.id,s.name,ps.role FROM project_design_styles ps "
                +"JOIN design_styles s ON s.id=ps.style_id WHERE ps.project_id IN (:ids) "
                +"ORDER BY ps.project_id,CASE WHEN ps.role='PRIMARY' THEN 0 ELSE 1 END,s.name",params)) {
            long id=((Number)row.get("project_id")).longValue();
            var item=result.get(id);
            if(item!=null) {
                @SuppressWarnings("unchecked")
                var styles=(List<Map<String,Object>>)item.get("styles");
                styles.add(Map.of("id",row.get("id"),"name",row.get("name"),"role",row.get("role")));
            }
        }
        return result;
    }
    @Transactional
    public void saveMetadata(Long projectId,MetadataInput input,boolean isNew) {
        if(input==null) input=new MetadataInput(null,null,null,List.of());
        List<Long> secondary=input.secondaryStyleIds()==null?List.of():input.secondaryStyleIds();
        if(secondary.size()>2 || new HashSet<>(secondary).size()!=secondary.size() || (input.primaryStyleId()!=null && secondary.contains(input.primaryStyleId())))
            throw new IllegalArgumentException("Choose one primary style and up to two different secondary styles");
        BigDecimal floorArea = input.floorAreaSqm();
        if (floorArea != null && (floorArea.signum() <= 0 || floorArea.scale() > 2
                || floorArea.compareTo(MAX_FLOOR_AREA_SQM) > 0)) {
            throw new IllegalArgumentException("Enter a positive floor area in m² (up to two decimal places)");
        }
        if(isNew) {
            if(count("SELECT count(*) FROM design_styles",Map.of())>0 && input.primaryStyleId()==null) throw new IllegalArgumentException("Select a primary design style");
            if(count("SELECT count(*) FROM property_types",Map.of())>0 && input.propertyTypeId()==null) throw new IllegalArgumentException("Select a property type");
        }
        if(input.primaryStyleId()!=null) requireIds("design_styles",List.of(input.primaryStyleId()));
        requireIds("design_styles",secondary);
        if(input.propertyTypeId()!=null) requireIds("property_types",List.of(input.propertyTypeId()));
        update("INSERT INTO project_metadata(project_id,property_type_id,floor_area_sqm) VALUES(:id,:type,:area) ON CONFLICT(project_id) DO UPDATE SET property_type_id=excluded.property_type_id, floor_area_sqm=excluded.floor_area_sqm",args("id",projectId,"type",input.propertyTypeId(),"area",input.floorAreaSqm()));
        update("DELETE FROM project_design_styles WHERE project_id=:id",args("id",projectId));
        if(input.primaryStyleId()!=null) update("INSERT INTO project_design_styles(project_id,style_id,role) VALUES(:id,:style,'PRIMARY')",args("id",projectId,"style",input.primaryStyleId()));
        for(Long id:secondary) update("INSERT INTO project_design_styles(project_id,style_id,role) VALUES(:id,:style,'SECONDARY')",args("id",projectId,"style",id));
    }
    private Set<Long> selections(String table,String column,Long customerId) {
        return rows("SELECT "+column+" FROM "+table+" WHERE customer_id=:id",args("id",customerId)).stream()
                .map(r->((Number)r.get(column)).longValue()).collect(Collectors.toSet());
    }
    @Transactional(readOnly=true)
    public Map<String,Object> ownMetadata(Long id,Authentication auth){
        var designer=signedIn(auth,UserRole.INTERIOR_DESIGNER);
        if(count("SELECT count(*) FROM interior_design_projects WHERE id=:id AND designer_id=:designer",args("id",id,"designer",designer.getId()))==0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Project not found");
        return metadata(id);
    }
    @Transactional(readOnly=true)
    public Map<String,Object> preferences(Authentication auth) {
        long id=signedIn(auth,UserRole.CUSTOMER).getId();
        var state=rows("SELECT onboarding_state,size_range_id FROM customer_recommendation_preferences WHERE customer_id=:id",args("id",id));
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("onboardingState",state.isEmpty()?"LEGACY":state.get(0).get("onboarding_state"));
        out.put("sizeRangeId",state.isEmpty()?null:state.get(0).get("size_range_id"));
        out.put("categoryIds",selections("customer_preferred_categories","category_id",id));
        out.put("styleIds",selections("customer_preferred_styles","style_id",id));
        out.put("propertyTypeIds",selections("customer_preferred_property_types","property_type_id",id));
        return out;
    }
    @Transactional
    public Map<String,Object> savePreferences(Authentication auth,PreferencesInput input) {
        long id=signedIn(auth,UserRole.CUSTOMER).getId();
        List<Long> cats=input.categoryIds()==null?List.of():input.categoryIds(), styles=input.styleIds()==null?List.of():input.styleIds(), types=input.propertyTypeIds()==null?List.of():input.propertyTypeIds();
        requireIds("project_categories",cats);requireIds("design_styles",styles);requireIds("property_types",types);
        if(input.sizeRangeId()!=null) requireIds("size_ranges",List.of(input.sizeRangeId()));
        update("INSERT INTO customer_recommendation_preferences(customer_id,size_range_id,onboarding_state) VALUES(:id,:range,'COMPLETED') ON CONFLICT(customer_id) DO UPDATE SET size_range_id=excluded.size_range_id,onboarding_state='COMPLETED'",args("id",id,"range",input.sizeRangeId()));
        saveSelection(id,"customer_preferred_categories","category_id",cats);
        saveSelection(id,"customer_preferred_styles","style_id",styles);
        saveSelection(id,"customer_preferred_property_types","property_type_id",types);
        return preferences(auth);
    }
    private void saveSelection(long id,String table,String field,List<Long> values) {
        update("DELETE FROM "+table+" WHERE customer_id=:id",args("id",id));
        for(Long v:values) update("INSERT INTO "+table+"(customer_id,"+field+") VALUES(:id,:v)",args("id",id,"v",v));
    }
    @Transactional
    public void skip(Authentication auth) {
        long id=signedIn(auth,UserRole.CUSTOMER).getId();
        update("INSERT INTO customer_recommendation_preferences(customer_id,onboarding_state) VALUES(:id,'SKIPPED') ON CONFLICT(customer_id) DO UPDATE SET onboarding_state=CASE WHEN customer_recommendation_preferences.onboarding_state='COMPLETED' THEN 'COMPLETED' ELSE 'SKIPPED' END",args("id",id));
    }
    /** SQL-side bounded search used by existing public project search without changing its array contract. */
    @Transactional(readOnly=true)
    public Set<Long> filteredIds(Long propertyType,Long sizeRange,List<Long> styles) {
        String sql="SELECT p.id FROM interior_design_projects p LEFT JOIN project_metadata m ON m.project_id=p.id WHERE 1=1";
        Map<String,Object> p=new HashMap<>();
        if(propertyType!=null){sql+=" AND m.property_type_id=:type";p.put("type",propertyType);}
        if(sizeRange!=null) {sql+=" AND m.floor_area_sqm IS NOT NULL AND EXISTS (SELECT 1 FROM size_ranges r WHERE r.id=:range AND m.floor_area_sqm >= COALESCE(r.min_sqm,0) AND (r.max_sqm IS NULL OR m.floor_area_sqm < r.max_sqm))";p.put("range",sizeRange);}
        if(styles!=null && !styles.isEmpty()){sql+=" AND EXISTS (SELECT 1 FROM project_design_styles ps WHERE ps.project_id=p.id AND ps.style_id IN (:styles))";p.put("styles",styles);}
        return rows(sql,p).stream().map(r->((Number)r.get("id")).longValue()).collect(Collectors.toSet());
    }
    private record Project(long id,long designerId,String title,String description,String status,Long categoryId,String categoryName,Long propertyTypeId,BigDecimal area,Long primary,Set<Long> secondary,Long timestamp,long popularity,List<String> photos,String designerName) {}
    private record Taste(Set<Long> cats,Set<Long> styles,Set<Long> types,Long size) {
        boolean present(){return !cats.isEmpty()||!styles.isEmpty()||!types.isEmpty()||size!=null;}
    }
    /** Explore is general photo discovery, never a personalised recommendation list. */
    @Transactional(readOnly=true)
    public Map<String,Object> explore(Authentication auth,int page,int limit,Set<Long> excluded) {
        return projectFeed(auth,page,limit,true,excluded,true);
    }
    @Transactional(readOnly=true)
    public Map<String,Object> recommendations(Authentication auth,int page,int limit,boolean reel) {
        return recommendations(auth,page,limit,reel,Set.of());
    }
    @Transactional(readOnly=true)
    public Map<String,Object> recommendations(Authentication auth,int page,int limit,boolean reel,Set<Long> excluded) {
        return projectFeed(auth,page,limit,reel,excluded,false);
    }
    private Map<String,Object> projectFeed(Authentication auth,int page,int limit,boolean reel,Set<Long> excluded,boolean exploreMode) {
        Long customer=optionalCustomer(auth);
        int take=limit==10000?10000:Math.min(20,Math.max(1,limit));int index=Math.max(0,Math.min(10000,page));
        var pRows=rows("""
                SELECT p.id,p.designer_id,p.title,p.description,p.status,p.category_id,cat.name category_name,p.created_at,
                m.property_type_id,m.floor_area_sqm,c.company_name,u.name designer_name,
                COALESCE(v.view_count,0) view_count,
                (SELECT count(*) FROM customer_project_favourites fav WHERE fav.project_id=p.id) favourite_count
                FROM interior_design_projects p JOIN user_accounts u ON u.id=p.designer_id AND u.active=true AND u.role='INTERIOR_DESIGNER'
                LEFT JOIN project_categories cat ON cat.id=p.category_id
                LEFT JOIN project_metadata m ON m.project_id=p.id LEFT JOIN designer_companies c ON c.designer_id=p.designer_id
                LEFT JOIN project_view_counts v ON v.project_id=p.id
                ORDER BY p.created_at DESC,p.id DESC
                """,Map.of());
        if(pRows.isEmpty()) return Map.of("items",List.of(),"page",index,"hasMore",false,"personalised",false);
        var ids=pRows.stream().map(r->((Number)r.get("id")).longValue()).toList();
        var photos=rows("SELECT project_id,id,display_order FROM project_images WHERE project_id IN (:ids) ORDER BY display_order,id",args("ids",ids));
        Map<Long,List<String>> images=new HashMap<>();
        for(var r:photos) {long pid=((Number)r.get("project_id")).longValue();images.computeIfAbsent(pid,k->new ArrayList<>()).add("/api/projects/"+pid+"/images/"+r.get("id"));}
        var ps=rows("SELECT project_id,style_id,role FROM project_design_styles WHERE project_id IN (:ids)",args("ids",ids));
        Map<Long,Long> prim=new HashMap<>();Map<Long,Set<Long>> sec=new HashMap<>();
        for(var r:ps){long pid=((Number)r.get("project_id")).longValue(),sid=((Number)r.get("style_id")).longValue();if("PRIMARY".equals(r.get("role")))prim.put(pid,sid);else sec.computeIfAbsent(pid,k->new HashSet<>()).add(sid);}
        List<Project> projects=new ArrayList<>();
        for(var r:pRows){long id=((Number)r.get("id")).longValue();
            projects.add(new Project(id,((Number)r.get("designer_id")).longValue(),(String)r.get("title"),(String)r.get("description"),(String)r.get("status"),nlong(r.get("category_id")),(String)r.get("category_name"),nlong(r.get("property_type_id")),(BigDecimal)r.get("floor_area_sqm"),prim.get(id),sec.getOrDefault(id,Set.of()),epoch(r.get("created_at")),((Number)r.get("view_count")).longValue()+3*((Number)r.get("favourite_count")).longValue(),images.getOrDefault(id,List.of()),(String)(r.get("company_name")==null?r.get("designer_name"):r.get("company_name"))));
        }
        Set<Long> saved=customer==null?Set.of():selections("customer_project_favourites","project_id",customer);
        Taste explicit=new Taste(Set.of(),Set.of(),Set.of(),null);
        if(!exploreMode && customer!=null){var state=rows("SELECT size_range_id FROM customer_recommendation_preferences WHERE customer_id=:id",args("id",customer));
            explicit=new Taste(selections("customer_preferred_categories","category_id",customer),selections("customer_preferred_styles","style_id",customer),selections("customer_preferred_property_types","property_type_id",customer),state.isEmpty()?null:nlong(state.get(0).get("size_range_id")));
        }
        final Taste selected=explicit;
        var sizeRows=exploreMode ? List.<Map<String,Object>>of() : rows("SELECT id,min_sqm,max_sqm FROM size_ranges",Map.of());
        Map<Long,List<BigDecimal>> ranges=new HashMap<>();for(var r:sizeRows)ranges.put(((Number)r.get("id")).longValue(),Arrays.asList((BigDecimal)r.get("min_sqm"),(BigDecimal)r.get("max_sqm")));
        List<Project> favourites=exploreMode ? List.of() : projects.stream().filter(p->saved.contains(p.id()) && informative(p)).toList();
        boolean personalised=!exploreMode && (selected.present()||!favourites.isEmpty());
        double alpha=selected.present()?Math.min(.40,.08*favourites.size()):1.0;
        Map<Long,Double> scores=new HashMap<>();
        for(Project p:projects){
            if(!personalised){scores.put(p.id(),0.0);continue;}
            Double explicitScore=selected.present()?similarity(selected,p,ranges):null;
            Double favouriteScore=favourites.isEmpty()?null:favourites.stream().mapToDouble(f->similarity(new Taste(f.categoryId()==null?Set.of():Set.of(f.categoryId()),merged(f),f.propertyTypeId()==null?Set.of():Set.of(f.propertyTypeId()),rangeOf(f.area(),ranges)),p,ranges)).average().orElse(.5);
            scores.put(p.id(),explicitScore==null?favouriteScore:favouriteScore==null?explicitScore:RecommendationMath.blend(explicitScore,favouriteScore,favourites.size()));
        }
        Comparator<Project> rank=personalised?Comparator.comparingDouble((Project p)->scores.get(p.id())).reversed().thenComparingLong(Project::id):
            Comparator.comparingLong((Project p)->p.timestamp()/86400000).reversed().thenComparing(Comparator.comparingLong(Project::popularity).reversed()).thenComparingLong(Project::id);
        // The Explore feed is independent of customer tastes: recent/popular/photo-rich
        // projects with light variety. Only personalised recommendations exclude saved projects.
        List<Project> candidates=projects.stream().filter(p->!excluded.contains(p.id()))
                .filter(p->exploreMode || limit==10000 || !personalised || !saved.contains(p.id()))
                .filter(p->!reel || !p.photos().isEmpty()).toList();
        List<Project> available;
        if(exploreMode) {
            var order=ExploreRanking.order(candidates.stream().map(p->new ExploreRanking.Candidate(
                    p.id(),p.designerId(),p.categoryId(),p.timestamp(),p.popularity())).toList());
            Map<Long,Project> lookup=candidates.stream().collect(Collectors.toMap(Project::id,p->p));
            available=order.stream().map(c->lookup.get(c.id())).toList();
        } else {
            available=candidates.stream().sorted(rank).toList();
        }
        int start=Math.min(available.size(),index*take),end=Math.min(available.size(),start+take);
        List<Map<String,Object>> items=new ArrayList<>();
        for(Project p:available.subList(start,end)){
            var m=new LinkedHashMap<String,Object>();m.put("id",p.id());m.put("title",p.title());m.put("description",p.description());m.put("status",p.status());m.put("designerId",p.designerId());m.put("designerName",p.designerName());m.put("categoryName",p.categoryName());m.put("photos",p.photos());m.put("metadata",metadata(p.id()));if(!exploreMode) m.put("score",personalised?Math.round(scores.get(p.id())*100):null);m.put("saved",saved.contains(p.id()));items.add(m);
        }
        return Map.of("items",items,"page",index,"hasMore",end<available.size(),"personalised",personalised);
    }
    private static long epoch(Object timestamp) {
        if(timestamp instanceof java.util.Date d) return d.getTime();
        if(timestamp instanceof java.time.OffsetDateTime d) return d.toInstant().toEpochMilli();
        if(timestamp instanceof java.time.LocalDateTime d) return d.toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
        return 0L;
    }
    private static Long nlong(Object n){return n==null?null:((Number)n).longValue();}
    private static Set<Long> merged(Project p){Set<Long> s=new HashSet<>(p.secondary());if(p.primary()!=null)s.add(p.primary());return s;}
    private static boolean informative(Project p){return p.primary()!=null||!p.secondary().isEmpty()||p.categoryId()!=null||p.propertyTypeId()!=null||p.area()!=null;}
    private static Long rangeOf(BigDecimal value,Map<Long,List<BigDecimal>> ranges){if(value==null)return null;return ranges.entrySet().stream().filter(e->inside(value,e.getValue())).map(Map.Entry::getKey).findFirst().orElse(null);}
    private static boolean inside(BigDecimal a,List<BigDecimal> bounds){return a!=null&&(bounds.get(0)==null||a.compareTo(bounds.get(0))>=0)&&(bounds.get(1)==null||a.compareTo(bounds.get(1))<0);}
    private static double attribute(Set<Long> preferred,Long actual){return preferred.isEmpty()?Double.NaN:actual==null?.5:preferred.contains(actual)?1:0;}
    private static double similarity(Taste t,Project p,Map<Long,List<BigDecimal>> ranges){
        double sum=0,weights=0;
        if(!t.styles().isEmpty()) {double x=p.primary()!=null&&t.styles().contains(p.primary())?1:p.secondary().stream().anyMatch(t.styles()::contains)?.6:p.primary()==null&&p.secondary().isEmpty()?.5:0;sum+=.5*x;weights+=.5;}
        double c=attribute(t.cats(),p.categoryId());if(!Double.isNaN(c)){sum+=.25*c;weights+=.25;}
        double pt=attribute(t.types(),p.propertyTypeId());if(!Double.isNaN(pt)){sum+=.15*pt;weights+=.15;}
        if(t.size()!=null){double a=p.area()==null?.5:inside(p.area(),ranges.getOrDefault(t.size(),Arrays.asList(null,null)))?1:0;sum+=.1*a;weights+=.1;}
        return RecommendationMath.preference(sum,weights);
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> designerRecommendations(Authentication auth,int limit) {
        var projects=recommendations(auth,0,10000,false); // bounded internally: use pages in a future scalability pass
        var all=new ArrayList<Map<String,Object>>((List<Map<String,Object>>)projects.get("items"));
        Map<Long,List<Double>> scores=new HashMap<>();Map<Long,String> names=new HashMap<>();
        for(var p:all){long id=((Number)p.get("designerId")).longValue();scores.computeIfAbsent(id,k->new ArrayList<>()).add(p.get("score")==null?0.0:((Number)p.get("score")).doubleValue());names.put(id,(String)p.get("designerName"));}
        return scores.entrySet().stream().map(e->{double avg=RecommendationMath.topThreeAverage(e.getValue());Map<String,Object> m=new LinkedHashMap<>();m.put("id",e.getKey());m.put("name",names.get(e.getKey()));m.put("score",projects.get("personalised").equals(true)?Math.round(avg):null);return m;}).sorted(Comparator.<Map<String,Object>>comparingDouble(m->m.get("score")==null?0:((Number)m.get("score")).doubleValue()).reversed().thenComparingLong(m->((Number)m.get("id")).longValue())).limit(Math.max(1,Math.min(20,limit))).toList();
    }
}
